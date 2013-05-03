/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;

/**
 *
 */
final class SourceBuilder {
    private SourceBuilder() {
    }

    public static String ToSource(ExecutionContext cx, Object val) {
        HashSet<ScriptObject> stack = new HashSet<>();
        return toSource(cx, stack, val);
    }

    private static String toSource(ExecutionContext cx, Set<ScriptObject> stack, Object value) {
        switch (Type.of(value)) {
        case Null:
            return "null";
        case Boolean:
            return Type.booleanValue(value) ? "true" : "false";
        case String:
            return stringToSource(Type.stringValue(value));
        case Number:
            return ToFlatString(cx, value);
        case Object:
            ScriptObject objValue = Type.objectValue(value);
            Object toSource = Get(cx, objValue, "toSource");
            if (IsCallable(toSource)) {
                return ToFlatString(cx, ((Callable) toSource).call(cx, objValue));
            }
            if (IsCallable(objValue)) {
                return ((Callable) objValue).toSource();
            }
            if (objValue instanceof Symbol) {
                return ((Symbol) objValue).toString();
            }
            if (stack.contains(objValue)) {
                return "« ... »";
            }
            stack.add(objValue);
            try {
                if (objValue instanceof ExoticArray) {
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

    private static String objectToSource(ExecutionContext cx, Set<ScriptObject> stack,
            ScriptObject value) {
        List<String> keys = GetOwnPropertyKeys(cx, value);
        if (keys.isEmpty()) {
            return "{}";
        }
        StringBuilder properties = new StringBuilder();
        for (String k : keys) {
            String p = toSource(cx, stack, Get(cx, value, k));
            properties.append(',').append(k).append(':').append(p);
        }
        properties.append('}').setCharAt(0, '{');
        return properties.toString();
    }

    private static String arrayToSource(ExecutionContext cx, Set<ScriptObject> stack,
            ScriptObject value) {
        long len = ToUint32(cx, Get(cx, value, "length"));
        if (len <= 0) {
            return "[]";
        }
        StringBuilder properties = new StringBuilder();
        for (long index = 0; index < len; ++index) {
            String p = toSource(cx, stack, Get(cx, value, ToString(index)));
            properties.append(',').append(p);
        }
        properties.append(']').setCharAt(0, '[');
        return properties.toString();
    }
}
