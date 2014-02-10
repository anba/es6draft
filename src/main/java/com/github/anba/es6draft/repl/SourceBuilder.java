/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.objects.DateObject;
import com.github.anba.es6draft.runtime.objects.DatePrototype;
import com.github.anba.es6draft.runtime.objects.RegExpObject;
import com.github.anba.es6draft.runtime.objects.RegExpPrototype;
import com.github.anba.es6draft.runtime.types.Callable;
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

    private SourceBuilder() {
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

        private final int on;
        private final int off;

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

    public enum Mode {
        Simple, Color
    }

    /**
     * Returns the simple mode, source representation for {@code val}
     */
    public static String ToSource(ExecutionContext cx, Object val) {
        return ToSource(Mode.Simple, cx, val);
    }

    /**
     * Returns the source representation for {@code val}
     */
    public static String ToSource(Mode mode, ExecutionContext cx, Object val) {
        HashSet<ScriptObject> stack = new HashSet<>();
        return toSource(mode, cx, stack, val);
    }

    private static String toSource(Mode mode, ExecutionContext cx, Set<ScriptObject> stack,
            Object value) {
        if (Type.isObject(value)) {
            ScriptObject objValue = Type.objectValue(value);
            Object toSource = Get(cx, objValue, "toSource");
            if (IsCallable(toSource)) {
                return ToFlatString(cx, ((Callable) toSource).call(cx, objValue));
            }
        }
        return format(mode, source(mode, cx, stack, value), style(stack, value));
    }

    private static String format(Mode mode, String source, Style style) {
        if (mode == Mode.Simple || style == null) {
            return source;
        }
        return String.format("\u001B[%dm%s\u001B[%d;%dm", style.on, source,
                AnsiAttribute.Reset.code, style.off);
    }

    private static Style style(Set<ScriptObject> stack, Object value) {
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

    private static String source(Mode mode, ExecutionContext cx, Set<ScriptObject> stack,
            Object value) {
        switch (Type.of(value)) {
        case Null:
            return "null";
        case Boolean:
            return Type.booleanValue(value) ? "true" : "false";
        case String:
            return stringToSource(Type.stringValue(value));
        case Symbol:
            return Type.symbolValue(value).toString();
        case Number:
            return ToFlatString(cx, value);
        case Object:
            ScriptObject objValue = Type.objectValue(value);
            if (IsCallable(objValue)) {
                return ((Callable) objValue).toSource();
            }
            if (stack.contains(objValue) || stack.size() > MAX_STACK_DEPTH) {
                return "« ... »";
            }
            stack.add(objValue);
            try {
                if (objValue instanceof DateObject) {
                    return DatePrototype.Properties.toString(cx, value).toString();
                } else if (objValue instanceof RegExpObject) {
                    return RegExpPrototype.Properties.toString(cx, value).toString();
                } else if (objValue instanceof ExoticArray) {
                    return arrayToSource(mode, cx, stack, objValue);
                } else {
                    return objectToSource(mode, cx, stack, objValue);
                }
            } finally {
                stack.remove(objValue);
            }
        case Undefined:
        default:
            return "(void 0)";
        }
    }

    private static final char[] hexdigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f' };

    private static String stringToSource(CharSequence value) {
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0, len = value.length(); i < len; ++i) {
            char c = value.charAt(i);
            switch (c) {
            case '"':
            case '\\':
                sb.append('\\').append(c);
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\r':
                sb.append("\\r");
                break;
            case '\t':
                sb.append("\\t");
                break;
            default:
                if (c < 0x20 || c > 0xff) {
                    sb.append('\\').append('u').append(hexdigits[(c >> 12) & 0xf])
                            .append(hexdigits[(c >> 8) & 0xf]).append(hexdigits[(c >> 4) & 0xf])
                            .append(hexdigits[(c >> 0) & 0xf]);
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private static final Pattern namePattern = Pattern
            .compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");

    private static String propertyKeyToSource(Mode mode, Object key) {
        if (key instanceof String) {
            String s = (String) key;
            if (namePattern.matcher(s).matches()) {
                return s;
            }
            return format(mode, stringToSource(s), Style.String);
        }
        assert key instanceof Symbol;
        String description = ((Symbol) key).getDescription();
        if (description == null) {
            description = "Symbol()";
        }
        return format(mode, String.format("[%s]", description), Style.Symbol);
    }

    private static String objectToSource(Mode mode, ExecutionContext cx, Set<ScriptObject> stack,
            ScriptObject value) {
        List<Object> keys = GetOwnEnumerablePropertyKeys(cx, value);
        if (keys.isEmpty()) {
            return "{}";
        }
        List<Object> view = keys.subList(0, Math.min(keys.size(), MAX_OBJECT_PROPERTIES));
        StringBuilder properties = new StringBuilder();
        for (Object k : view) {
            String key = propertyKeyToSource(mode, k);
            String p = toSource(mode, cx, stack, Get(cx, value, k));
            properties.append(", ").append(key).append(": ").append(p);
        }
        if (view.size() < keys.size()) {
            properties.append(", [...]");
        }
        properties.append(" }").setCharAt(0, '{');
        return properties.toString();
    }

    private static String arrayToSource(Mode mode, ExecutionContext cx, Set<ScriptObject> stack,
            ScriptObject value) {
        long len = ToUint32(cx, Get(cx, value, "length"));
        if (len <= 0) {
            return "[]";
        }
        int viewLen = (int) Math.min(len, MAX_ARRAY_PROPERTIES);
        StringBuilder properties = new StringBuilder();
        for (int index = 0; index < viewLen; ++index) {
            String p = toSource(mode, cx, stack, Get(cx, value, ToString(index)));
            properties.append(", ").append(p);
        }
        if (viewLen < len) {
            properties.append(", [...]");
        }
        properties.append(" ]").setCharAt(0, '[');
        return properties.toString();
    }
}
