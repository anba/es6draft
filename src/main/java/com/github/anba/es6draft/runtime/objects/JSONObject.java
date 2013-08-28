/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwSyntaxError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.parser.JSONParser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;
import com.github.anba.es6draft.runtime.types.builtins.ExoticString;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.12 The JSON Object</h2>
 * <ul>
 * <li>15.12.2 JSON.parse (text [, reviver])
 * <li>15.12.3 JSON.stringify (value [, replacer [, space]])
 * </ul>
 */
public class JSONObject extends OrdinaryObject implements Initialisable {
    public JSONObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.12.2 JSON.parse ( text [ , reviver ] )
         */
        @Function(name = "parse", arity = 2)
        public static Object parse(ExecutionContext cx, Object thisValue, Object text,
                Object reviver) {
            /* steps 1-2 */
            CharSequence jtext = ToString(cx, text);
            /* steps 3-6 */
            Object unfiltered;
            try {
                JSONParser parser = new JSONParser(cx, jtext);
                unfiltered = parser.parse();
            } catch (ParserException e) {
                throw throwSyntaxError(cx, Messages.Key.InvalidJSONLiteral);
            }
            /* step 7 */
            if (IsCallable(reviver)) {
                ScriptObject root = ObjectCreate(cx, Intrinsics.ObjectPrototype);
                CreateOwnDataProperty(cx, root, "", unfiltered);
                return Walk(cx, (Callable) reviver, root, "");
            }
            /* step 8 */
            return unfiltered;
        }

        /**
         * 15.12.3 JSON.stringify ( value [ , replacer [ , space ] ] )
         */
        @Function(name = "stringify", arity = 3)
        public static Object stringify(ExecutionContext cx, Object thisValue, Object value,
                Object replacer, Object space) {
            /* step 1 */
            HashSet<ScriptObject> stack = new HashSet<>();
            /* step 2 */
            String indent = "";
            /* step 3 */
            LinkedHashSet<String> propertyList = null;
            Callable replacerFunction = null;
            /* step 4 */
            if (Type.isObject(replacer)) {
                if (IsCallable(replacer)) {
                    replacerFunction = (Callable) replacer;
                } else if (replacer instanceof ExoticArray) {
                    // https://bugs.ecmascript.org/show_bug.cgi?id=170
                    propertyList = new LinkedHashSet<>();
                    ScriptObject objReplacer = Type.objectValue(replacer);
                    long len = ToLength(cx, Get(cx, objReplacer, "length"));
                    for (long i = 0; i < len; ++i) {
                        String item = null;
                        Object v = Get(cx, objReplacer, ToString(i));
                        if (Type.isString(v)) {
                            item = Type.stringValue(v).toString();
                        } else if (Type.isNumber(v)) {
                            item = ToString(Type.numberValue(v));
                        } else if (Type.isObject(v)) {
                            ScriptObject o = Type.objectValue(v);
                            if (o instanceof ExoticString || o instanceof NumberObject) {
                                item = ToFlatString(cx, v);
                            }
                        }
                        if (item != null && !propertyList.contains(item)) {
                            propertyList.add(item);
                        }
                    }
                }
            }
            /* step 5 */
            if (Type.isObject(space)) {
                ScriptObject o = Type.objectValue(space);
                if (o instanceof NumberObject) {
                    space = ToNumber(cx, space);
                } else if (o instanceof ExoticString) {
                    space = ToString(cx, space);
                }
            }
            /* steps 6-8 */
            String gap;
            if (Type.isNumber(space)) {
                int nspace = (int) Math.max(0, Math.min(10, ToInteger(Type.numberValue(space))));
                char[] a = new char[nspace];
                Arrays.fill(a, ' ');
                gap = new String(a);
            } else if (Type.isString(space)) {
                String sspace = Type.stringValue(space).toString();
                gap = sspace.length() <= 10 ? sspace : sspace.substring(0, 10);
            } else {
                gap = "";
            }
            /* step 9 */
            ScriptObject wrapper = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            /* step 10 */
            CreateOwnDataProperty(cx, wrapper, "", value);
            /* step 11 */
            String result = Str(cx, stack, propertyList, replacerFunction, indent, gap, "", wrapper);
            if (result == null) {
                return UNDEFINED;
            }
            return result;
        }
    }

    /**
     * Runtime Semantics: Walk Abstract Operation
     */
    public static Object Walk(ExecutionContext cx, Callable reviver, ScriptObject holder,
            String name) {
        /* steps 1-2 */
        Object val = Get(cx, holder, name);
        /* step 3 */
        if (Type.isObject(val)) {
            ScriptObject objVal = Type.objectValue(val);
            if (objVal instanceof ExoticArray) {
                /* step 3.a */
                long len = ToLength(cx, Get(cx, objVal, "length"));
                for (long i = 0; i < len; ++i) {
                    Object newElement = Walk(cx, reviver, objVal, ToString(i));
                    if (Type.isUndefined(newElement)) {
                        objVal.delete(cx, ToString(i));
                    } else {
                        objVal.defineOwnProperty(cx, ToString(i), new PropertyDescriptor(
                                newElement, true, true, true));
                    }
                }
            } else {
                /* step 3.b */
                Iterable<String> keys = GetOwnEnumerablePropertyNames(cx, objVal);
                for (String p : keys) {
                    Object newElement = Walk(cx, reviver, objVal, p);
                    if (Type.isUndefined(newElement)) {
                        objVal.delete(cx, p);
                    } else {
                        objVal.defineOwnProperty(cx, p, new PropertyDescriptor(newElement, true,
                                true, true));
                    }
                }
            }
        }
        /* step 4 */
        return reviver.call(cx, holder, name, val);
    }

    /**
     * Runtime Semantics: Str Abstract Operation
     */
    public static String Str(ExecutionContext cx, Set<ScriptObject> stack,
            Set<String> propertyList, Callable replacerFunction, String indent, String gap,
            String key, ScriptObject holder) {
        /* steps 1-2 */
        Object value = Get(cx, holder, key);
        /* step 3 */
        if (Type.isObject(value)) {
            ScriptObject objValue = Type.objectValue(value);
            Object toJSON = Get(cx, objValue, "toJSON");
            if (IsCallable(toJSON)) {
                value = ((Callable) toJSON).call(cx, value, key);
            }
        }
        /* step 4 */
        if (replacerFunction != null) {
            value = replacerFunction.call(cx, holder, key, value);
        }
        /* step 5 */
        if (Type.isObject(value)) {
            ScriptObject o = Type.objectValue(value);
            if (o instanceof NumberObject) {
                value = ToNumber(cx, value);
            } else if (o instanceof ExoticString) {
                value = ToString(cx, value);
            } else if (o instanceof BooleanObject) {
                value = ((BooleanObject) value).getBooleanData();
            }
        }
        /* steps 6-12 */
        switch (Type.of(value)) {
        case Null:
            return "null";
        case Boolean:
            return Type.booleanValue(value) ? "true" : "false";
        case String:
            return Quote(Type.stringValue(value));
        case Number:
            return isFinite(Type.numberValue(value)) ? ToFlatString(cx, value) : "null";
        case Object:
            if (!IsCallable(value)) {
                if (value instanceof ExoticArray) {
                    return JA(cx, stack, propertyList, replacerFunction, indent, gap,
                            Type.objectValue(value));
                } else {
                    return JO(cx, stack, propertyList, replacerFunction, indent, gap,
                            Type.objectValue(value));
                }
            } else {
                return null;
            }
        case Undefined:
        default:
            return null;
        }
    }

    private static boolean isFinite(double v) {
        return !(Double.isNaN(v) || Double.isInfinite(v));
    }

    private static final char[] hexdigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Runtime Semantics: Quote Abstract Operation
     */
    public static String Quote(CharSequence value) {
        StringBuilder product = new StringBuilder(value.length() + 2);
        /* step 1 */
        product.append('"');
        /* step 2 */
        for (int i = 0, len = value.length(); i < len; ++i) {
            char c = value.charAt(i);
            switch (c) {
            case '"':
            case '\\':
                product.append('\\').append(c);
                break;
            case '\b':
                product.append('\\').append('b');
                break;
            case '\f':
                product.append('\\').append('f');
                break;
            case '\n':
                product.append('\\').append('n');
                break;
            case '\r':
                product.append('\\').append('r');
                break;
            case '\t':
                product.append('\\').append('t');
                break;
            default:
                if (c < ' ') {
                    product.append('\\').append('u')//
                            .append(hexdigits[(c >> 12) & 0xf])//
                            .append(hexdigits[(c >> 8) & 0xf])//
                            .append(hexdigits[(c >> 4) & 0xf])//
                            .append(hexdigits[(c >> 0) & 0xf]);
                } else {
                    product.append(c);
                }
            }
        }
        /* step 3 */
        product.append('"');
        /* step 4 */
        return product.toString();
    }

    /**
     * Runtime Semantics: JO Abstract Operation
     */
    public static String JO(ExecutionContext cx, Set<ScriptObject> stack, Set<String> propertyList,
            Callable replacerFunction, String indent, String gap, ScriptObject value) {
        /* step 1 */
        if (stack.contains(value)) {
            throw throwTypeError(cx, Messages.Key.CyclicValue);
        }
        /* step 2 */
        stack.add(value);
        /* step 3 */
        String stepback = indent;
        /* step 4 */
        indent = indent + gap;
        /* steps 5-6 */
        Iterable<String> k;
        if (propertyList != null) {
            k = propertyList;
        } else {
            k = GetOwnEnumerablePropertyNames(cx, value);
        }
        /* step 7 */
        List<String> partial = new ArrayList<>();
        /* step 8 */
        for (String p : k) {
            String strP = Str(cx, stack, propertyList, replacerFunction, indent, gap, p, value);
            if (strP != null) {
                StringBuilder member = new StringBuilder(p.length() + strP.length() + 4);
                member.append(Quote(p)).append(":");
                if (!gap.isEmpty()) {
                    member.append(' ');
                }
                member.append(strP);
                partial.add(member.toString());
            }
        }
        /* steps 9-10 */
        String _final;
        if (partial.isEmpty()) {
            _final = "{}";
        } else {
            if (gap.isEmpty()) {
                StringBuilder properties = new StringBuilder();
                for (String p : partial) {
                    properties.append(',').append(p);
                }
                properties.append('}').setCharAt(0, '{');
                _final = properties.toString();
            } else {
                StringBuilder properties = new StringBuilder();
                String separator = ",\n" + indent;
                for (String p : partial) {
                    properties.append(separator).append(p);
                }
                properties.append('\n').append(stepback).append('}').setCharAt(0, '{');
                _final = properties.toString();
            }
        }
        /* step 11 */
        stack.remove(value);
        /* step 12 (not applicable) */
        /* step 13 */
        return _final;
    }

    /**
     * Runtime Semantics: JA Abstract Operation
     */
    public static String JA(ExecutionContext cx, Set<ScriptObject> stack, Set<String> propertyList,
            Callable replacerFunction, String indent, String gap, ScriptObject value) {
        /* step 1 */
        if (stack.contains(value)) {
            throw throwTypeError(cx, Messages.Key.CyclicValue);
        }
        /* step 2 */
        stack.add(value);
        /* step 3 */
        String stepback = indent;
        /* step 4 */
        indent = indent + gap;
        /* step 5 */
        List<String> partial = new ArrayList<>();
        /* steps 6-7 */
        Object lenVal = Get(cx, value, "length");
        /* steps 8-9 */
        long len = ToLength(cx, lenVal);
        /* steps 10-11 */
        for (long index = 0; index < len; ++index) {
            String strP = Str(cx, stack, propertyList, replacerFunction, indent, gap,
                    ToString(index), value);
            if (strP == null) {
                partial.add("null");
            } else {
                partial.add(strP);
            }
        }
        /* steps 12-13 */
        String _final;
        if (partial.isEmpty()) {
            _final = "[]";
        } else {
            if (gap.isEmpty()) {
                StringBuilder properties = new StringBuilder();
                for (String p : partial) {
                    properties.append(',').append(p);
                }
                properties.append(']').setCharAt(0, '[');
                _final = properties.toString();
            } else {
                StringBuilder properties = new StringBuilder();
                String separator = ",\n" + indent;
                for (String p : partial) {
                    properties.append(separator).append(p);
                }
                properties.append('\n').append(stepback).append(']').setCharAt(0, '[');
                _final = properties.toString();
            }
        }
        /* step 14 */
        stack.remove(value);
        /* step 15 (not applicable) */
        /* step 16 */
        return _final;
    }
}
