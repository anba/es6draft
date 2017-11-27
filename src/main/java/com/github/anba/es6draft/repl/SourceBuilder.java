/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToUint32;

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
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;

/**
 *
 */
public final class SourceBuilder {
    private static final int MAX_STACK_DEPTH = 5;
    private static final int MAX_OBJECT_PROPERTIES = 30;
    private static final int MAX_ARRAY_PROPERTIES = 80;
    private static final SourceBuilder INSTANCE = new SourceBuilder();

    private final boolean colored;
    private final int maxStackDepth;
    private final int maxObjectProperties;
    private final int maxArrayProperties;

    SourceBuilder() {
        this(false, MAX_STACK_DEPTH, MAX_OBJECT_PROPERTIES, MAX_ARRAY_PROPERTIES);
    }

    SourceBuilder(boolean colored, int maxStackDepth, int maxObjectProperties, int maxArrayProperties) {
        this.colored = colored;
        this.maxStackDepth = maxStackDepth;
        this.maxObjectProperties = maxObjectProperties;
        this.maxArrayProperties = maxArrayProperties;
    }

    private enum Style {
        Special(Ansi.Attribute.TextColorCyan),

        Number(Ansi.Attribute.TextColorYellow),

        Boolean(Ansi.Attribute.TextColorYellow),

        Undefined(Ansi.Attribute.TextColorHiBlack),

        Null(Ansi.Attribute.Bold),

        String(Ansi.Attribute.TextColorGreen),

        Symbol(Ansi.Attribute.TextColorGreen),

        SIMD(Ansi.Attribute.TextColorYellow),

        Date(Ansi.Attribute.TextColorMagenta),

        RegExp(Ansi.Attribute.TextColorRed);

        final Ansi.Attribute attr;

        private Style(Ansi.Attribute attr) {
            this.attr = attr;
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
        return style.attr + source + Ansi.Attribute.Reset;
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
        case BigInt:
            return Style.Number;
        case Symbol:
            return Style.Symbol;
        case SIMD:
            return Style.SIMD;
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
        case BigInt:
            return ToFlatString(cx, value) + "n";
        case SIMD:
            return Type.simdValue(value).toString();
        case Object:
            ScriptObject objValue = Type.objectValue(value);
            if (IsCallable(objValue)) {
                return ((Callable) objValue).toSource(cx);
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
                } else if (objValue instanceof ArrayObject) {
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

    private String arrayToSource(ExecutionContext cx, HashSet<ScriptObject> stack, ScriptObject array) {
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

    private String objectToSource(ExecutionContext cx, HashSet<ScriptObject> stack, ScriptObject object) {
        Iterator<?> keys = object.ownPropertyKeys(cx).iterator();
        if (!keys.hasNext()) {
            return "{}";
        }
        StringBuilder properties = new StringBuilder();
        for (int i = 0; keys.hasNext() && i < maxObjectProperties;) {
            Object k = keys.next();
            String key = propertyKeyToSource(cx, k);
            Property prop;
            try {
                prop = object.getOwnProperty(cx, k);
            } catch (ScriptException e) {
                continue;
            }
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

    private static final Pattern namePattern = Pattern
            .compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");

    private String propertyKeyToSource(ExecutionContext cx, Object key) {
        if (key instanceof String) {
            String s = (String) key;
            if (namePattern.matcher(s).matches()) {
                return s;
            }
            return format(Strings.quote(s), Style.String);
        }
        assert key instanceof Symbol;
        return String.format("[%s]", format(symbolDescription(cx, (Symbol) key), Style.Symbol));
    }

    private static String symbolDescription(ExecutionContext cx, Symbol symbol) {
        for (BuiltinSymbol builtin : BuiltinSymbol.values()) {
            if (builtin != BuiltinSymbol.NONE && builtin.get() == symbol) {
                return symbol.getDescription();
            }
        }
        String registered = cx.getRealm().getSymbolRegistry().getKey(symbol);
        if (registered != null) {
            return String.format("Symbol.for(%s)", Strings.quote(registered));
        }
        String description = symbol.getDescription();
        if (description != null) {
            return String.format("Symbol(%s)", Strings.quote(description));
        }
        return "Symbol()";
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
}
