/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.objects.date.DateObject;
import com.github.anba.es6draft.runtime.objects.date.DatePrototype;
import com.github.anba.es6draft.runtime.objects.text.RegExpObject;
import com.github.anba.es6draft.runtime.objects.text.RegExpPrototype;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;

/**
 *
 */
public final class SourceBuilder {
    private static final int MAX_STACK_DEPTH = 5;
    private static final int MAX_OBJECT_PROPERTIES = 30;
    private static final int MAX_ARRAY_PROPERTIES = 80;
    private static final SourceBuilder INSTANCE = new SourceBuilder(false);

    private final boolean colored;
    private final int maxStackDepth;
    private final int maxObjectProperties;
    private final int maxArrayProperties;

    SourceBuilder(boolean colored) {
        this(colored, MAX_STACK_DEPTH, MAX_OBJECT_PROPERTIES, MAX_ARRAY_PROPERTIES);
    }

    SourceBuilder(boolean colored, int maxStackDepth, int maxObjectProperties,
            int maxArrayProperties) {
        this.colored = colored;
        this.maxStackDepth = maxStackDepth;
        this.maxObjectProperties = maxObjectProperties;
        this.maxArrayProperties = maxArrayProperties;
    }

    private enum AnsiAttribute {
        Reset(0), Bold(1), Underline(4), Negative(7), NormalIntensity(22), UnderlineNone(24),
        Positive(27), TextColor(30), DefaultTextColor(39), BackgroundColor(40),
        DefaultBackgroundColor(49), TextColorHi(90), BackgroundColorHi(100);

        final int code;

        private AnsiAttribute(int code) {
            this.code = code;
        }

        int color(AnsiColor color) {
            return code + color.offset;
        }
    }

    private enum AnsiColor {
        Black(0), Red(1), Green(2), Yellow(3), Blue(4), Magenta(5), Cyan(6), White(7);

        final int offset;

        private AnsiColor(int offset) {
            this.offset = offset;
        }
    }

    private enum Style {/* @formatter:off */
        Special(AnsiAttribute.TextColor.color(AnsiColor.Cyan), AnsiAttribute.DefaultTextColor),
        Number(AnsiAttribute.TextColor.color(AnsiColor.Yellow), AnsiAttribute.DefaultTextColor),
        Boolean(AnsiAttribute.TextColor.color(AnsiColor.Yellow), AnsiAttribute.DefaultTextColor),
        Undefined(AnsiAttribute.TextColorHi.color(AnsiColor.Black), AnsiAttribute.DefaultTextColor),
        Null(AnsiAttribute.Bold, AnsiAttribute.NormalIntensity),
        String(AnsiAttribute.TextColor.color(AnsiColor.Green), AnsiAttribute.DefaultTextColor),
        Symbol(AnsiAttribute.TextColor.color(AnsiColor.Green), AnsiAttribute.DefaultTextColor),
        Date(AnsiAttribute.TextColor.color(AnsiColor.Magenta), AnsiAttribute.DefaultTextColor),
        RegExp(AnsiAttribute.TextColor.color(AnsiColor.Red), AnsiAttribute.DefaultTextColor),
        ;
        /* @formatter:on */

        final int on, off;

        private Style(AnsiAttribute on, AnsiAttribute off) {
            this(on.code, off.code);
        }

        private Style(int on, AnsiAttribute off) {
            this(on, off.code);
        }

        private Style(int on, int off) {
            this.on = on;
            this.off = off;
        }
    }

    /**
     * Returns the simple mode, source representation for {@code val}.
     * 
     * @param cx
     *            the execution context
     * @param val
     *            the value
     * @return the source representation of the value
     */
    public static String ToSource(ExecutionContext cx, Object val) {
        return INSTANCE.toSource(cx, val);
    }

    /**
     * Returns the source representation for {@code val}.
     * 
     * @param cx
     *            the execution context
     * @param val
     *            the value
     * @return the source representation of the value
     */
    String toSource(ExecutionContext cx, Object val) {
        HashSet<ScriptObject> stack = new HashSet<>();
        return toSource(cx, stack, val);
    }

    private String toSource(ExecutionContext cx, HashSet<ScriptObject> stack, Object value) {
        if (Type.isObject(value)) {
            ScriptObject objValue = Type.objectValue(value);
            Object toSource = Get(cx, objValue, "toSource");
            if (IsCallable(toSource)) {
                return ToFlatString(cx, ((Callable) toSource).call(cx, objValue));
            }
        }
        return format(source(cx, stack, value), style(stack, value));
    }

    private String format(String source, Style style) {
        if (!colored || style == null) {
            return source;
        }
        return String.format("\u001B[%dm%s\u001B[%d;%dm", style.on, source,
                AnsiAttribute.Reset.code, style.off);
    }

    private static Style style(HashSet<ScriptObject> stack, Object value) {
        switch (Type.of(value)) {
        case Undefined:
            return Style.Undefined;
        case Null:
            return Style.Null;
        case Boolean:
            return Style.Boolean;
        case String:
            return Style.String;
        case Number:
            return Style.Number;
        case Symbol:
            return Style.Symbol;
        case Object:
        default:
            if (IsCallable(value)) {
                return Style.Special;
            }
            if (stack.contains(value)) {
                return Style.Special;
            }
            if (value instanceof DateObject) {
                return Style.Date;
            }
            if (value instanceof RegExpObject) {
                return Style.RegExp;
            }
            return null;
        }
    }

    private String source(ExecutionContext cx, HashSet<ScriptObject> stack, Object value) {
        switch (Type.of(value)) {
        case Null:
            return "null";
        case Boolean:
            return Type.booleanValue(value) ? "true" : "false";
        case String:
            return Strings.quote(Type.stringValue(value).toString());
        case Symbol:
            return Type.symbolValue(value).toString();
        case Number:
            return ToFlatString(cx, value);
        case Object:
            ScriptObject objValue = Type.objectValue(value);
            if (IsCallable(objValue)) {
                return ((Callable) objValue).toSource(Callable.SourceSelector.Function);
            }
            if (stack.contains(objValue) || stack.size() > maxStackDepth) {
                return "« ... »";
            }
            stack.add(objValue);
            try {
                if (objValue instanceof DateObject) {
                    return DatePrototype.Properties.toString(cx, value).toString();
                } else if (objValue instanceof RegExpObject) {
                    return RegExpPrototype.Properties.toString(cx, value).toString();
                } else if (objValue instanceof ExoticArray) {
                    return arrayToSource(cx, stack, objValue);
                } else {
                    return objectToSource(cx, stack, objValue);
                }
            } finally {
                stack.remove(objValue);
            }
        case Undefined:
        default:
            return "(void 0)";
        }
    }

    private static final Pattern namePattern = Pattern
            .compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");

    private String propertyKeyToSource(Object key) {
        if (key instanceof String) {
            String s = (String) key;
            if (namePattern.matcher(s).matches()) {
                return s;
            }
            return format(Strings.quote(s), Style.String);
        }
        assert key instanceof Symbol;
        String description = ((Symbol) key).getDescription();
        if (description == null) {
            description = "Symbol()";
        }
        return format(String.format("[%s]", description), Style.Symbol);
    }

    private static Property getOwnProperty(ExecutionContext cx, ScriptObject object, Object key) {
        try {
            if (key instanceof String) {
                return object.getOwnProperty(cx, (String) key);
            } else {
                return object.getOwnProperty(cx, (Symbol) key);
            }
        } catch (ScriptException e) {
            return null;
        }
    }

    private String accessorToSource(Property accessor) {
        String description;
        if (accessor.getGetter() != null && accessor.getSetter() != null) {
            description = "[Getter/Setter]";
        } else if (accessor.getGetter() != null) {
            description = "[Getter]";
        } else if (accessor.getSetter() != null) {
            description = "[Setter]";
        } else {
            description = "[]";
        }
        return format(description, Style.Special);
    }

    private String objectToSource(ExecutionContext cx, HashSet<ScriptObject> stack,
            ScriptObject object) {
        Iterator<?> keys = object.ownKeys(cx);
        if (!keys.hasNext()) {
            return "{}";
        }
        StringBuilder properties = new StringBuilder();
        for (int i = 0; keys.hasNext() && i < maxObjectProperties;) {
            Object k = ToPropertyKey(cx, keys.next());
            String key = propertyKeyToSource(k);
            Property prop = getOwnProperty(cx, object, k);
            if (prop == null || !prop.isEnumerable()) {
                continue;
            }
            String value;
            if (prop.isDataDescriptor()) {
                value = toSource(cx, stack, prop.getValue());
            } else {
                value = accessorToSource(prop);
            }
            properties.append(", ").append(key).append(": ").append(value);
            i += 1;
        }
        if (keys.hasNext()) {
            properties.append(", [...]");
        }
        properties.append(" }").setCharAt(0, '{');
        return properties.toString();
    }

    private String arrayToSource(ExecutionContext cx, HashSet<ScriptObject> stack,
            ScriptObject array) {
        long len = ToUint32(cx, Get(cx, array, "length"));
        if (len <= 0) {
            return "[]";
        }
        int viewLen = (int) Math.min(len, maxArrayProperties);
        StringBuilder properties = new StringBuilder();
        for (int index = 0; index < viewLen; ++index) {
            String value = toSource(cx, stack, Get(cx, array, index));
            properties.append(", ").append(value);
        }
        if (viewLen < len) {
            properties.append(", [...]");
        }
        properties.append(" ]").setCharAt(0, '[');
        return properties.toString();
    }
}
