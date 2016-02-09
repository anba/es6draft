/**
 * Copyright (c) 2012-2015 André Bargull
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

import java.util.HashSet;
import java.util.LinkedHashSet;

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
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
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
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
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
        public static Object parse(ExecutionContext cx, Object thisValue, Object text, Object reviver) {
            /* steps 1-2 */
            String jtext = ToFlatString(cx, text);
            /* steps 3-7 */
            Object unfiltered;
            try {
                unfiltered = JSONParser.parse(cx, jtext);
            } catch (ParserException e) {
                throw newSyntaxError(cx, e, Messages.Key.JSONInvalidLiteral, e.getFormattedMessage(cx.getRealm()),
                        Integer.toString(e.getLine()), Integer.toString(e.getColumn()));
            }
            /* step 8 */
            if (IsCallable(reviver)) {
                OrdinaryObject root = ObjectCreate(cx, Intrinsics.ObjectPrototype);
                String rootName = "";
                boolean status = CreateDataProperty(cx, root, rootName, unfiltered);
                assert status;
                return InternalizeJSONProperty(cx, (Callable) reviver, root, rootName);
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
        public static Object stringify(ExecutionContext cx, Object thisValue, Object value, Object replacer,
                Object space) {
            /* steps 1-2 (not applicable) */
            /* step 3 */
            LinkedHashSet<String> propertyList = null;
            Callable replacerFunction = null;
            /* step 4 */
            if (Type.isObject(replacer)) {
                if (IsCallable(replacer)) {
                    replacerFunction = (Callable) replacer;
                } else if (IsArray(cx, replacer)) {
                    propertyList = new LinkedHashSet<>();
                    ScriptObject objReplacer = (ScriptObject) replacer;
                    long len = ToLength(cx, Get(cx, objReplacer, "length"));
                    for (long k = 0; k < len; ++k) {
                        String item = null;
                        Object v = Get(cx, objReplacer, k);
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
            JSONSerializer serializer = new JSONSerializer(propertyList, replacerFunction, gap);
            value = TransformJSONValue(cx, serializer, wrapper, "", value);
            if (!IsJSONSerializable(value)) {
                return UNDEFINED;
            }
            SerializeJSONValue(cx, serializer, value);
            return serializer.result.toString();
        }

        /**
         * 24.3.3 JSON [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "JSON";
    }

    /**
     * 24.3.1.1 Runtime Semantics: InternalizeJSONProperty( holder, name)
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
    private static Object InternalizeJSONProperty(ExecutionContext cx, Callable reviver, ScriptObject holder,
            String name) {
        /* steps 1-2 */
        Object val = Get(cx, holder, name);
        /* step 3 */
        if (Type.isObject(val)) {
            InternalizeJSONValue(cx, reviver, Type.objectValue(val));
        }
        /* step 4 */
        return reviver.call(cx, holder, name, val);
    }

    /**
     * 24.3.1.1 Runtime Semantics: InternalizeJSONProperty( holder, name)
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
    private static Object InternalizeJSONProperty(ExecutionContext cx, Callable reviver, ScriptObject holder,
            long name) {
        /* steps 1-2 */
        Object val = Get(cx, holder, name);
        /* step 3 */
        if (Type.isObject(val)) {
            InternalizeJSONValue(cx, reviver, Type.objectValue(val));
        }
        /* step 4 */
        return reviver.call(cx, holder, ToString(name), val);
    }

    private static void InternalizeJSONValue(ExecutionContext cx, Callable reviver, ScriptObject val) {
        /* InternalizeJSONProperty, step 3 */
        /* steps 3.a-b */
        boolean isArray = IsArray(cx, val);
        /* steps 3.c-d */
        if (isArray) {
            /* step 3.c */
            long len = ToLength(cx, Get(cx, val, "length"));
            for (long i = 0; i < len; ++i) {
                Object newElement = InternalizeJSONProperty(cx, reviver, val, i);
                if (Type.isUndefined(newElement)) {
                    val.delete(cx, i);
                } else {
                    CreateDataProperty(cx, val, i, newElement);
                }
            }
        } else {
            /* step 3.d */
            for (String p : EnumerableOwnNames(cx, val)) {
                Object newElement = InternalizeJSONProperty(cx, reviver, val, p);
                if (Type.isUndefined(newElement)) {
                    val.delete(cx, p);
                } else {
                    CreateDataProperty(cx, val, p, newElement);
                }
            }
        }
    }

    private static final class JSONSerializer {
        final HashSet<ScriptObject> stack;
        final HashSet<String> propertyList;
        final Callable replacerFunction;
        final String gap;
        final StringBuilder result = new StringBuilder();
        int level = 0;

        JSONSerializer(HashSet<String> propertyList, Callable replacerFunction, String gap) {
            this.stack = new HashSet<>();
            this.propertyList = propertyList;
            this.replacerFunction = replacerFunction;
            this.gap = gap;
        }
    }

    /**
     * 24.3.2.1 Runtime Semantics: SerializeJSONProperty (key, holder )
     * 
     * @param cx
     *            the execution context
     * @param serializer
     *            the serializer state
     * @param holder
     *            the script object
     * @param key
     *            the property key
     * @param value
     *            the property value
     * @return the transformed property value
     */
    private static Object TransformJSONValue(ExecutionContext cx, JSONSerializer serializer, ScriptObject holder,
            String key, Object value) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        if (Type.isObject(value)) {
            Object toJSON = Get(cx, Type.objectValue(value), "toJSON");
            if (IsCallable(toJSON)) {
                value = ((Callable) toJSON).call(cx, value, key);
            }
        }
        /* step 4 */
        if (serializer.replacerFunction != null) {
            value = serializer.replacerFunction.call(cx, holder, key, value);
        }
        return value;
    }

    /**
     * 24.3.2.1 Runtime Semantics: SerializeJSONProperty (key, holder )
     * 
     * @param cx
     *            the execution context
     * @param serializer
     *            the serializer state
     * @param value
     *            the property value
     */
    private static void SerializeJSONValue(ExecutionContext cx, JSONSerializer serializer, Object value) {
        /* steps 1-4 (not applicable) */
        /* steps 5-12 */
        switch (Type.of(value)) {
        case Null:
            SerializeJSONNull(serializer);
            return;
        case Boolean:
            SerializeJSONBoolean(serializer, Type.booleanValue(value));
            return;
        case String:
            SerializeJSONString(serializer, Type.stringValue(value));
            return;
        case Number:
            SerializeJSONNumber(serializer, Type.numberValue(value));
            return;
        case Object:
            assert !IsCallable(value);
            ScriptObject valueObj = Type.objectValue(value);
            if (valueObj instanceof NumberObject) {
                SerializeJSONNumber(serializer, ToNumber(cx, value));
            } else if (valueObj instanceof StringObject) {
                SerializeJSONString(serializer, ToString(cx, value));
            } else if (valueObj instanceof BooleanObject) {
                SerializeJSONBoolean(serializer, ((BooleanObject) valueObj).getBooleanData());
            } else if (IsArray(cx, valueObj)) {
                SerializeJSONArray(cx, serializer, valueObj);
            } else {
                SerializeJSONObject(cx, serializer, valueObj);
            }
            return;
        case Undefined:
        case Symbol:
        case SIMD:
        default:
            throw new AssertionError();
        }
    }

    private static void SerializeJSONNull(JSONSerializer serializer) {
        serializer.result.append("null");
    }

    private static void SerializeJSONBoolean(JSONSerializer serializer, boolean b) {
        serializer.result.append(b);
    }

    private static void SerializeJSONNumber(JSONSerializer serializer, double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            serializer.result.append("null");
        } else {
            serializer.result.append(ToString(v));
        }
    }

    private static void SerializeJSONString(JSONSerializer serializer, CharSequence string) {
        QuoteJSONString(serializer.result, string.toString());
    }

    private static boolean IsJSONSerializable(Object value) {
        switch (Type.of(value)) {
        case Boolean:
        case Null:
        case String:
        case Number:
            return true;
        case Object:
            return !IsCallable(value);
        case Undefined:
        case Symbol:
        case SIMD:
            return false;
        default:
            throw new AssertionError();
        }
    }

    /* @formatter:off */
    private static final char[] HEXDIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    /* @formatter:on */

    /**
     * 24.3.2.2 Runtime Semantics: QuoteJSONString ( value )
     * 
     * @param product
     *            the output string builder
     * @param value
     *            the string
     */
    private static void QuoteJSONString(StringBuilder product, String value) {
        product.ensureCapacity(value.length() + 2);
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
                    /* @formatter:off */
                    product.append('\\').append('u')
                            .append(HEXDIGITS[(c >> 12) & 0xf])
                            .append(HEXDIGITS[(c >> 8) & 0xf])
                            .append(HEXDIGITS[(c >> 4) & 0xf])
                            .append(HEXDIGITS[(c >> 0) & 0xf]);
                    /* @formatter:on */
                } else {
                    product.append(c);
                }
            }
        }
        /* step 3 */
        product.append('"');
        /* step 4 (not applicable) */
    }

    /**
     * 24.3.2.3 Runtime Semantics: SerializeJSONObject ( value )
     * 
     * @param cx
     *            the execution context
     * @param serializer
     *            the serializer state
     * @param value
     *            the script object
     */
    private static void SerializeJSONObject(ExecutionContext cx, JSONSerializer serializer, ScriptObject value) {
        /* steps 1-2 */
        if (!serializer.stack.add(value)) {
            throw newTypeError(cx, Messages.Key.JSONCyclicValue);
        }
        /* steps 3-4 (not applicable) */
        /* steps 5-6 */
        Iterable<String> k;
        if (serializer.propertyList != null) {
            k = serializer.propertyList;
        } else {
            k = EnumerableOwnNames(cx, value);
        }
        /* step 7 (not applicable) */
        /* steps 8-10 */
        boolean isEmpty = true;
        String gap = serializer.gap;
        StringBuilder result = serializer.result;
        result.append('{');
        serializer.level += 1;
        for (String p : k) {
            // Inlined: SerializeJSONProperty
            Object v = Get(cx, value, p);
            v = TransformJSONValue(cx, serializer, value, p, v);
            if (!IsJSONSerializable(v)) {
                continue;
            }
            if (!isEmpty) {
                result.append(',');
            }
            isEmpty = false;
            if (!gap.isEmpty()) {
                indent(serializer, result);
            }
            QuoteJSONString(result, p);
            result.append(':');
            if (!gap.isEmpty()) {
                result.append(' ');
            }
            SerializeJSONValue(cx, serializer, v);
        }
        serializer.level -= 1;
        if (!isEmpty && !gap.isEmpty()) {
            indent(serializer, result);
        }
        result.append('}');
        /* step 11 */
        serializer.stack.remove(value);
        /* steps 12-13 (not applicable) */
    }

    /**
     * 24.3.2.4 Runtime Semantics: SerializeJSONArray( value )
     * 
     * @param cx
     *            the execution context
     * @param serializer
     *            the serializer state
     * @param value
     *            the script array object
     * @param stack
     *            the current stack
     */
    private static void SerializeJSONArray(ExecutionContext cx, JSONSerializer serializer, ScriptObject value) {
        /* steps 1-2 */
        if (!serializer.stack.add(value)) {
            throw newTypeError(cx, Messages.Key.JSONCyclicValue);
        }
        /* steps 3-5 (not applicable) */
        /* steps 6-7 */
        long len = ToLength(cx, Get(cx, value, "length"));
        /* steps 8-11 */
        String gap = serializer.gap;
        StringBuilder result = serializer.result;
        result.append('[');
        if (len > 0) {
            serializer.level += 1;
            for (long index = 0; index < len; ++index) {
                if (!gap.isEmpty()) {
                    indent(serializer, result);
                }
                // Inlined: SerializeJSONProperty
                Object v = Get(cx, value, index);
                v = TransformJSONValue(cx, serializer, value, ToString(index), v);
                if (!IsJSONSerializable(v)) {
                    result.append("null");
                } else {
                    SerializeJSONValue(cx, serializer, v);
                }
                if (index + 1 < len) {
                    result.append(',');
                }
            }
            serializer.level -= 1;
            if (!gap.isEmpty()) {
                indent(serializer, result);
            }
        }
        result.append(']');
        /* step 12 */
        serializer.stack.remove(value);
        /* steps 13-14 (not applicable) */
    }

    private static void indent(JSONSerializer serializer, StringBuilder sb) {
        int level = serializer.level;
        String gap = serializer.gap;
        sb.ensureCapacity(1 + level * gap.length());
        sb.append('\n');
        for (int i = 0; i < level; ++i) {
            sb.append(gap);
        }
    }
}
