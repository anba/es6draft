/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newSyntaxError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.github.anba.es6draft.parser.JSONParser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.objects.number.NumberObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.github.anba.es6draft.runtime.types.builtins.StringObject;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.3 The JSON Object</h2>
 * <ul>
 * <li>24.3.2 JSON.parse (text [, reviver])
 * <li>24.3.3 JSON.stringify (value [, replacer [, space]])
 * </ul>
 */
public final class JSONObject extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new JSON object.
     * 
     * @param realm
     *            the realm object
     */
    public JSONObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 24.3.1 JSON.parse ( text [ , reviver ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param text
         *            the JSON text
         * @param reviver
         *            the optional reviver argument
         * @return the parsed JSON value
         */
        @Function(name = "parse", arity = 2)
        public static Object parse(ExecutionContext cx, Object thisValue, Object text,
                Object reviver) {
            /* steps 1-2 */
            String jtext = ToFlatString(cx, text);
            /* steps 3-7 */
            Object unfiltered;
            try {
                unfiltered = JSONParser.parse(cx, jtext);
            } catch (ParserException e) {
                throw newSyntaxError(cx, Messages.Key.JSONInvalidLiteral);
            }
            /* step 8 */
            if (IsCallable(reviver)) {
                OrdinaryObject root = ObjectCreate(cx, Intrinsics.ObjectPrototype);
                boolean status = CreateDataProperty(cx, root, "", unfiltered);
                assert status;
                return Walk(cx, (Callable) reviver, root, "");
            }
            /* step 9 */
            return unfiltered;
        }

        /**
         * 24.3.2 JSON.stringify ( value [ , replacer [ , space ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @param replacer
         *            the optional replacer argument
         * @param space
         *            the optional space argument
         * @return the JSON string
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
                } else if (replacer instanceof ArrayObject) {
                    // https://bugs.ecmascript.org/show_bug.cgi?id=170
                    propertyList = new LinkedHashSet<>();
                    ArrayObject objReplacer = (ArrayObject) replacer;
                    // long len = ToLength(cx, Get(cx, objReplacer, "length"));
                    long len = objReplacer.getLength();
                    for (long i = 0; i < len; ++i) {
                        String item = null;
                        Object v = Get(cx, objReplacer, i);
                        if (Type.isString(v)) {
                            item = Type.stringValue(v).toString();
                        } else if (Type.isNumber(v)) {
                            item = ToString(Type.numberValue(v));
                        } else if (Type.isObject(v)) {
                            ScriptObject o = Type.objectValue(v);
                            if (o instanceof StringObject || o instanceof NumberObject) {
                                item = ToFlatString(cx, v);
                            }
                        }
                        if (item != null) {
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
                } else if (o instanceof StringObject) {
                    space = ToString(cx, space);
                }
            }
            /* steps 6-8 */
            String gap;
            if (Type.isNumber(space)) {
                int nspace = (int) Math.max(0, Math.min(10, ToInteger(Type.numberValue(space))));
                gap = Strings.repeat(' ', nspace);
            } else if (Type.isString(space)) {
                String sspace = Type.stringValue(space).toString();
                gap = sspace.length() <= 10 ? sspace : sspace.substring(0, 10);
            } else {
                gap = "";
            }
            /* step 9 */
            OrdinaryObject wrapper = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            /* steps 10-11 */
            boolean status = CreateDataProperty(cx, wrapper, "", value);
            assert status;
            /* step 12 */
            String result = Str(cx, stack, propertyList, replacerFunction, indent, gap, "", wrapper);
            if (result == null) {
                return UNDEFINED;
            }
            return result;
        }

        /**
         * 24.3.3 JSON [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "JSON";
    }

    /**
     * 24.3.1.1 Runtime Semantics: Walk Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param reviver
     *            the reviver function
     * @param holder
     *            the script object
     * @param name
     *            the property key
     * @return the result value
     */
    public static Object Walk(ExecutionContext cx, Callable reviver, ScriptObject holder,
            String name) {
        /* steps 1-2 */
        Object val = Get(cx, holder, name);
        /* step 3 */
        if (Type.isObject(val)) {
            ScriptObject objVal = Type.objectValue(val);
            if (objVal instanceof ArrayObject) {
                /* step 3.a */
                Walk(cx, reviver, (ArrayObject) objVal);
            } else {
                /* step 3.b */
                Walk(cx, reviver, objVal);
            }
        }
        /* step 4 */
        return reviver.call(cx, holder, name, val);
    }

    /**
     * 24.3.1.1 Runtime Semantics: Walk Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param reviver
     *            the reviver function
     * @param holder
     *            the script object
     * @param name
     *            the property key
     * @return the result value
     */
    public static Object Walk(ExecutionContext cx, Callable reviver, ScriptObject holder, long name) {
        /* steps 1-2 */
        Object val = Get(cx, holder, name);
        /* step 3 */
        if (Type.isObject(val)) {
            ScriptObject objVal = Type.objectValue(val);
            if (objVal instanceof ArrayObject) {
                /* step 3.a */
                Walk(cx, reviver, (ArrayObject) objVal);
            } else {
                /* step 3.b */
                Walk(cx, reviver, objVal);
            }
        }
        /* step 4 */
        return reviver.call(cx, holder, ToString(name), val);
    }

    private static void Walk(ExecutionContext cx, Callable reviver, ArrayObject val) {
        /* step 3.a */
        // long len = ToLength(cx, Get(cx, val, "length"));
        long len = val.getLength();
        for (long i = 0; i < len; ++i) {
            Object newElement = Walk(cx, reviver, val, i);
            if (Type.isUndefined(newElement)) {
                val.delete(cx, i);
            } else {
                val.defineOwnProperty(cx, i, new PropertyDescriptor(newElement, true, true, true));
            }
        }
    }

    private static void Walk(ExecutionContext cx, Callable reviver, ScriptObject val) {
        /* step 3.b */
        for (String p : EnumerableOwnNames(cx, val)) {
            Object newElement = Walk(cx, reviver, val, p);
            if (Type.isUndefined(newElement)) {
                val.delete(cx, p);
            } else {
                val.defineOwnProperty(cx, p, new PropertyDescriptor(newElement, true, true, true));
            }
        }
    }

    /**
     * 24.3.2.1 Runtime Semantics: Str Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param stack
     *            the current stack
     * @param propertyList
     *            the set of property keys to visit
     * @param replacerFunction
     *            the replacer function
     * @param indent
     *            the current indentation
     * @param gap
     *            the string gap
     * @param key
     *            the property key
     * @param holder
     *            the script object
     * @return the JSON string
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
            } else if (o instanceof StringObject) {
                value = ToString(cx, value);
            } else if (o instanceof BooleanObject) {
                BooleanObject bool = (BooleanObject) o;
                if (!bool.isInitialized()) {
                    throw newTypeError(cx, Messages.Key.UninitializedObject);
                }
                value = bool.getBooleanData();
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
            double d = Type.numberValue(value);
            return isFinite(d) ? ToString(d) : "null";
        case Object:
            if (!IsCallable(value)) {
                if (value instanceof ArrayObject) {
                    return JA(cx, stack, propertyList, replacerFunction, indent, gap,
                            (ArrayObject) value);
                } else {
                    return JO(cx, stack, propertyList, replacerFunction, indent, gap,
                            Type.objectValue(value));
                }
            } else {
                return null;
            }
        case Undefined:
        case Symbol:
        default:
            return null;
        }
    }

    private static boolean isFinite(double v) {
        return !(Double.isNaN(v) || Double.isInfinite(v));
    }

    private static final char[] HEXDIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * 24.3.2.2 Runtime Semantics: Quote Abstract Operation
     * 
     * @param value
     *            the string
     * @return the quoted string
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
                            .append(HEXDIGITS[(c >> 12) & 0xf])//
                            .append(HEXDIGITS[(c >> 8) & 0xf])//
                            .append(HEXDIGITS[(c >> 4) & 0xf])//
                            .append(HEXDIGITS[(c >> 0) & 0xf]);
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
     * 24.3.2.3 Runtime Semantics: JO Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param stack
     *            the current stack
     * @param propertyList
     *            the set of property keys to visit
     * @param replacerFunction
     *            the replacer function
     * @param indent
     *            the current indentation
     * @param gap
     *            the string gap
     * @param value
     *            the script object
     * @return the JSON string
     */
    public static String JO(ExecutionContext cx, Set<ScriptObject> stack, Set<String> propertyList,
            Callable replacerFunction, String indent, String gap, ScriptObject value) {
        /* steps 1-2 */
        if (!stack.add(value)) {
            throw newTypeError(cx, Messages.Key.JSONCyclicValue);
        }
        /* step 3 */
        String stepback = indent;
        /* step 4 */
        indent = indent + gap;
        /* steps 5-6 */
        Iterable<String> k;
        if (propertyList != null) {
            k = propertyList;
        } else {
            k = EnumerableOwnNames(cx, value);
        }
        /* step 7 */
        ArrayList<String> partial = new ArrayList<>();
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
     * 24.3.2.4 Runtime Semantics: JA Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param stack
     *            the current stack
     * @param propertyList
     *            the set of property keys to visit
     * @param replacerFunction
     *            the replacer function
     * @param indent
     *            the current indentation
     * @param gap
     *            the string gap
     * @param value
     *            the script array object
     * @return the JSON string
     */
    public static String JA(ExecutionContext cx, Set<ScriptObject> stack, Set<String> propertyList,
            Callable replacerFunction, String indent, String gap, ArrayObject value) {
        /* steps 1-2 */
        if (!stack.add(value)) {
            throw newTypeError(cx, Messages.Key.JSONCyclicValue);
        }
        /* step 3 */
        String stepback = indent;
        /* step 4 */
        indent = indent + gap;
        /* step 5 */
        ArrayList<String> partial = new ArrayList<>();
        /* steps 6-9 */
        // long len = ToLength(cx, Get(cx, value, "length"));
        long len = value.getLength();
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
