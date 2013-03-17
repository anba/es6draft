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
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
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
public class JSONObject extends OrdinaryObject implements ScriptObject, Initialisable {
    public JSONObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinJSON;
    }

    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.12.2 JSON.parse ( text [ , reviver ] )
         */
        @Function(name = "parse", arity = 2)
        public static Object parse(Realm realm, Object thisValue, Object text, Object reviver) {
            CharSequence jtext = ToString(realm, text);
            Object unfiltered;
            try {
                JSONParser parser = new JSONParser(realm, jtext);
                unfiltered = parser.parse();
            } catch (ParserException e) {
                throw throwSyntaxError(realm, Messages.Key.InvalidJSONLiteral);
            }
            if (IsCallable(reviver)) {
                ScriptObject root = ObjectCreate(realm, Intrinsics.ObjectPrototype);
                CreateOwnDataProperty(root, "", unfiltered);
                return Walk(realm, (Callable) reviver, root, "");
            }
            return unfiltered;
        }

        /**
         * 15.12.3 JSON.stringify ( value [ , replacer [ , space ] ] )
         */
        @Function(name = "stringify", arity = 3)
        public static Object stringify(Realm realm, Object thisValue, Object value,
                Object replacer, Object space) {
            HashSet<ScriptObject> stack = new HashSet<>();
            String indent = "";
            LinkedHashSet<String> propertyList = null;
            Callable replacerFunction = null;
            if (Type.isObject(replacer)) {
                if (IsCallable(replacer)) {
                    replacerFunction = (Callable) replacer;
                } else if (Type.objectValue(replacer).getBuiltinBrand() == BuiltinBrand.BuiltinArray) {
                    // https://bugs.ecmascript.org/show_bug.cgi?id=170
                    propertyList = new LinkedHashSet<>();
                    ScriptObject objReplacer = Type.objectValue(replacer);
                    long len = ToUint32(realm, Get(objReplacer, "length"));
                    for (long i = 0; i < len; ++i) {
                        String item = null;
                        Object v = Get(objReplacer, ToString(i));
                        if (Type.isString(v)) {
                            item = Type.stringValue(v).toString();
                        } else if (Type.isNumber(v)) {
                            item = ToFlatString(realm, v);
                        } else if (Type.isObject(v)) {
                            ScriptObject o = Type.objectValue(v);
                            if (o instanceof ExoticString || o instanceof NumberObject) {
                                item = ToFlatString(realm, v);
                            }
                        }
                        if (item != null && !propertyList.contains(item)) {
                            propertyList.add(item);
                        }
                    }
                }
            }
            if (Type.isObject(space)) {
                ScriptObject o = Type.objectValue(space);
                if (o instanceof NumberObject) {
                    space = ToNumber(realm, space);
                } else if (o instanceof ExoticString) {
                    space = ToString(realm, space);
                }
            }
            String gap;
            if (Type.isNumber(space)) {
                int nspace = (int) Math.max(0,
                        Math.min(10, ToInteger(realm, Type.numberValue(space))));
                char[] a = new char[nspace];
                Arrays.fill(a, ' ');
                gap = new String(a);
            } else if (Type.isString(space)) {
                String sspace = Type.stringValue(space).toString();
                gap = sspace.length() <= 10 ? sspace : sspace.substring(0, 10);
            } else {
                gap = "";
            }
            ScriptObject wrapper = ObjectCreate(realm, Intrinsics.ObjectPrototype);
            CreateOwnDataProperty(wrapper, "", value);
            String result = Str(realm, stack, propertyList, replacerFunction, indent, gap, "",
                    wrapper);
            if (result == null) {
                return UNDEFINED;
            }
            return result;
        }
    }

    /**
     * Runtime Semantics: Walk Abstract Operation
     */
    public static Object Walk(Realm realm, Callable reviver, ScriptObject holder, String name) {
        Object val = Get(holder, name);
        if (Type.isObject(val)) {
            ScriptObject objVal = Type.objectValue(val);
            if (objVal.getBuiltinBrand() == BuiltinBrand.BuiltinArray) {
                long len = ToUint32(realm, Get(objVal, "length"));
                for (long i = 0; i < len; ++i) {
                    Object newElement = Walk(realm, reviver, objVal, ToString(i));
                    if (Type.isUndefined(newElement)) {
                        objVal.delete(ToString(i));
                    } else {
                        objVal.defineOwnProperty(ToString(i), new PropertyDescriptor(newElement,
                                true, true, true));
                    }
                }
            } else {
                Iterable<String> keys = GetOwnPropertyKeys(realm, objVal);
                for (String p : keys) {
                    Object newElement = Walk(realm, reviver, objVal, p);
                    if (Type.isUndefined(newElement)) {
                        objVal.delete(p);
                    } else {
                        objVal.defineOwnProperty(p, new PropertyDescriptor(newElement, true, true,
                                true));
                    }
                }
            }
        }
        return reviver.call(holder, name, val);
    }

    /**
     * Runtime Semantics: Str Abstract Operation
     */
    public static String Str(Realm realm, Set<ScriptObject> stack, Set<String> propertyList,
            Callable replacerFunction, String indent, String gap, String key, ScriptObject holder) {
        Object value = Get(holder, key);
        if (Type.isObject(value)) {
            ScriptObject objValue = Type.objectValue(value);
            Object toJSON = Get(objValue, "toJSON");
            if (IsCallable(toJSON)) {
                value = ((Callable) toJSON).call(value, key);
            }
        }
        if (replacerFunction != null) {
            value = replacerFunction.call(holder, key, value);
        }
        if (Type.isObject(value)) {
            ScriptObject o = Type.objectValue(value);
            if (o instanceof NumberObject) {
                value = ToNumber(realm, value);
            } else if (o instanceof ExoticString) {
                value = ToString(realm, value);
            } else if (o instanceof BooleanObject) {
                value = ((BooleanObject) value).getBooleanData();
            }
        }
        switch (Type.of(value)) {
        case Null:
            return "null";
        case Boolean:
            return Type.booleanValue(value) ? "true" : "false";
        case String:
            return Quote(Type.stringValue(value));
        case Number:
            return isFinite(Type.numberValue(value)) ? ToFlatString(realm, value) : "null";
        case Object:
            if (!IsCallable(value)) {
                if (Type.objectValue(value).getBuiltinBrand() == BuiltinBrand.BuiltinArray) {
                    return JA(realm, stack, propertyList, replacerFunction, indent, gap,
                            Type.objectValue(value));
                } else {
                    return JO(realm, stack, propertyList, replacerFunction, indent, gap,
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
        product.append('"');
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
        product.append('"');
        return product.toString();
    }

    /**
     * Runtime Semantics: JO Abstract Operation
     */
    public static String JO(Realm realm, Set<ScriptObject> stack, Set<String> propertyList,
            Callable replacerFunction, String indent, String gap, ScriptObject value) {
        if (stack.contains(value)) {
            throw throwTypeError(realm, Messages.Key.CyclicValue);
        }
        stack.add(value);
        String stepback = indent;
        indent = indent + gap;
        Iterable<String> k;
        if (propertyList != null) {
            k = propertyList;
        } else {
            // FIXME: spec bug (should possibly use [[Keys]]) (Bug 1142)
            k = GetOwnPropertyKeys(realm, value);
        }
        List<String> partial = new ArrayList<>();
        for (String p : k) {
            String strP = Str(realm, stack, propertyList, replacerFunction, indent, gap, p, value);
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
        stack.remove(value);
        return _final;
    }

    /**
     * Runtime Semantics: JA Abstract Operation
     */
    public static String JA(Realm realm, Set<ScriptObject> stack, Set<String> propertyList,
            Callable replacerFunction, String indent, String gap, ScriptObject value) {
        if (stack.contains(value)) {
            throw throwTypeError(realm, Messages.Key.CyclicValue);
        }
        stack.add(value);
        String stepback = indent;
        indent = indent + gap;
        List<String> partial = new ArrayList<>();
        long len = ToUint32(realm, Get(value, "length"));
        for (long index = 0; index < len; ++index) {
            String strP = Str(realm, stack, propertyList, replacerFunction, indent, gap,
                    ToString(index), value);
            if (strP == null) {
                partial.add("null");
            } else {
                partial.add(strP);
            }
        }
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
        stack.remove(value);
        return _final;
    }
}
