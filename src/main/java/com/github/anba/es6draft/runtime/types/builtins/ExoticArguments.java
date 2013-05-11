/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInt32;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.isStrictFunction;

import java.util.Arrays;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.5 Exotic Arguments Objects
 * </ul>
 * 
 * <h1>10 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>10.6 Arguments Object
 * </ul>
 */
public class ExoticArguments extends OrdinaryObject {
    /** [[ParameterMap]] */
    private ParameterMap parameterMap = null;

    private static class ParameterMap {
        private final LexicalEnvironment env;
        private final int length;
        private final String[] parameters;

        ParameterMap(ParameterMap original) {
            this.env = original.env;
            this.length = original.length;
            this.parameters = Arrays.copyOf(original.parameters, length, String[].class);
        }

        ParameterMap(LexicalEnvironment env, int length) {
            this.env = env;
            this.length = length;
            this.parameters = new String[length];
        }

        static int toArgumentIndex(String p) {
            final long limit = Integer.MAX_VALUE;
            int length = p.length();
            if (length < 1 || length > 10) {
                // empty string or definitely greater than "2147483647"
                // "2147483647".length == 10
                return -1;
            }
            if (p.charAt(0) == '0') {
                return (length == 1 ? 0 : -1);
            }
            long acc = 0L;
            for (int i = 0; i < length; ++i) {
                char c = p.charAt(i);
                if (!(c >= '0' && c <= '9')) {
                    return -1;
                }
                acc = acc * 10 + (c - '0');
            }
            return acc <= limit ? (int) acc : -1;
        }

        void defineOwnProperty(int propertyKey, String name) {
            parameters[propertyKey] = name;
        }

        Object get(String propertyKey) {
            int index = toArgumentIndex(propertyKey);
            if (index >= 0 && index < length) {
                String name = parameters[index];
                if (name != null) {
                    Object value = env.getEnvRec().getBindingValue(name, true);
                    return value;
                }
            }
            // TODO: assert false?
            return UNDEFINED;
        }

        void put(String propertyKey, Object value) {
            int index = toArgumentIndex(propertyKey);
            if (index >= 0 && index < length) {
                String name = parameters[index];
                if (name != null) {
                    env.getEnvRec().setMutableBinding(name, value, true);
                    return;
                }
            }
            // TODO: assert false?
        }

        boolean hasOwnProperty(String propertyKey) {
            int index = toArgumentIndex(propertyKey);
            if (index >= 0 && index < length) {
                return parameters[index] != null;
            }
            return false;
        }

        boolean delete(String propertyKey) {
            int index = toArgumentIndex(propertyKey);
            if (index >= 0 && index < length) {
                parameters[index] = null;
            }
            return true;
        }
    }

    public ExoticArguments(Realm realm) {
        super(realm);
    }

    /**
     * [10.6 Arguments Object] InstantiateArgumentsObject
     */
    public static ExoticArguments InstantiateArgumentsObject(ExecutionContext cx, Object[] args) {
        /* [10.6] step 1 */
        int len = args.length;
        /* [10.6] step 2-3 */
        ExoticArguments obj = new ExoticArguments(cx.getRealm());
        obj.setPrototype(cx, cx.getIntrinsic(Intrinsics.ObjectPrototype));
        /* [10.6] step 4 */
        obj.defineOwnProperty(cx, "length", new PropertyDescriptor(len, true, false, true));
        /* [10.6] step 5-6 */
        for (int index = 0; index < len; ++index) {
            Object val = args[index];
            obj.defineOwnProperty(cx, ToString(index),
                    new PropertyDescriptor(val, true, true, true));
        }
        return obj;
    }

    /**
     * [10.6 Arguments Object] CompleteStrictArgumentsObject
     */
    public static void CompleteStrictArgumentsObject(ExecutionContext cx, ExoticArguments obj) {
        /* [10.6] step 1-2 */
        AddRestrictedFunctionProperties(cx, obj);
    }

    /**
     * [13.6 Creating Function Objects and Constructors] AddRestrictedFunctionProperties
     */
    private static void AddRestrictedFunctionProperties(ExecutionContext cx, ExoticArguments obj) {
        /*  step 1  */
        Callable thrower = cx.getRealm().getThrowTypeError();
        /*  step 2  */
        obj.defineOwnProperty(cx, "caller", new PropertyDescriptor(thrower, thrower, false, false));
        /*  step 3  */
        // FIXME: spec bug ("arguments" per spec!) (Bug 1158)
        obj.defineOwnProperty(cx, "callee", new PropertyDescriptor(thrower, thrower, false, false));
    }

    /**
     * [10.6 Arguments Object] CompleteMappedArgumentsObject
     */
    public static void CompleteMappedArgumentsObject(ExecutionContext cx, ExoticArguments obj,
            FunctionObject func, String[] formals, LexicalEnvironment env) {
        // added ToInt32()
        int len = ToInt32(cx, Get(cx, obj, "length"));
        boolean hasMapped = false;
        int numberOfNonRestFormals = formals.length;
        ParameterMap map = new ParameterMap(env, len);
        // FIXME: spec bug duplicate arguments vs mapped arguments (bug 1240)
        for (int index = numberOfNonRestFormals - 1; index >= 0; --index) {
            String name = formals[index];
            if (name != null && index < len) {
                hasMapped = true;
                map.defineOwnProperty(index, name);
            }
        }
        if (hasMapped) {
            obj.parameterMap = map;
        }
        /*  step 9  */
        obj.defineOwnProperty(cx, "callee", new PropertyDescriptor(func, true, false, true));
    }

    public static ExoticArguments CreateLegacyArguments(ExecutionContext cx,
            ExoticArguments arguments, FunctionObject func) {
        int length = ToInt32(cx, Get(cx, arguments, "length"));
        ExoticLegacyArguments obj = new ExoticLegacyArguments(cx.getRealm());
        obj.ordinarySetPrototype(cx, cx.getIntrinsic(Intrinsics.ObjectPrototype));
        obj.ordinaryDefineOwnProperty("length", new PropertyDescriptor(length, true, false, true));
        obj.ordinaryDefineOwnProperty("callee", new PropertyDescriptor(func, true, false, true));
        for (int index = 0; index < length; ++index) {
            String pk = ToString(index);
            Object value = arguments.getOwnProperty(cx, pk).getValue();
            obj.ordinaryDefineOwnProperty(pk, new PropertyDescriptor(value, true, true, true));
        }
        if (arguments.parameterMap != null) {
            ((ExoticArguments) obj).parameterMap = new ParameterMap(arguments.parameterMap);
        }
        return obj;
    }

    private static class ExoticLegacyArguments extends ExoticArguments {
        public ExoticLegacyArguments(Realm realm) {
            super(realm);
        }

        boolean ordinarySetPrototype(ExecutionContext cx, ScriptObject prototype) {
            return super.setPrototype(cx, prototype);
        }

        @Override
        public boolean setPrototype(ExecutionContext cx, ScriptObject prototype) {
            // ignore attempts to change [[Prototype]]
            return true;
        }

        @Override
        public boolean setIntegrity(ExecutionContext cx, IntegrityLevel level) {
            // ignore attempts to change integrity level
            return true;
        }

        @Override
        public boolean delete(ExecutionContext cx, String propertyKey) {
            // this object is effectively unmodifiable
            return true;
        }

        @Override
        public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
                PropertyDescriptor desc) {
            // this object is effectively unmodifiable
            return true;
        }

        @Override
        public boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey,
                PropertyDescriptor desc) {
            // this object is effectively unmodifiable
            return true;
        }
    }

    /**
     * [[Get]]
     */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object accessorThisValue) {
        /*  step 1-2  */
        ParameterMap map = this.parameterMap;
        /*  [[ParameterMap]] not present  */
        if (map == null) {
            return super.get(cx, propertyKey, accessorThisValue);
        }
        /*  step 3  */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]]) (Bug 1412)
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /*  step 4  */
        if (!isMapped) {
            // FIXME: spec bug (does not work as intended) (Bug 1413)
            Object v = super.get(cx, propertyKey, accessorThisValue);
            if ("caller".equals(propertyKey) && isStrictFunction(v)) {
                throw throwTypeError(cx, Messages.Key.StrictModePoisonPill);
            }
            return v;
        }
        /*  step 5  */
        // return Get(map, propertyKey);
        return map.get(propertyKey);
    }

    /**
     * [[GetOwnProperty]]
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        /*  step 1  */
        Property desc = super.getOwnProperty(cx, propertyKey);
        /*  step 3  */
        if (desc == null) {
            return desc;
        }
        /*  step 3  */
        ParameterMap map = this.parameterMap;
        /*  [[ParameterMap]] not present  */
        if (map == null) {
            return desc;
        }
        /*  step 4  */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]]) (Bug 1412)
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /*  step 5  */
        if (isMapped) {
            // desc.setValue(Get(map, propertyKey));
            PropertyDescriptor d = desc.toPropertyDescriptor();
            d.setValue(map.get(propertyKey));
            desc = d.toProperty();
        }
        /*  step 6  */
        return desc;
    }

    /**
     * [[DefineOwnProperty]]
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        /*  step 1  */
        ParameterMap map = this.parameterMap;
        /*  [[ParameterMap]] not present  */
        if (map == null) {
            return super.defineOwnProperty(cx, propertyKey, desc);
        }
        /*  step 2  */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]]) (Bug 1412)
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /*  step 3-4  */
        boolean allowed = super.defineOwnProperty(cx, propertyKey, desc);
        /*  step 5  */
        if (!allowed) {
            return false;
        }
        /*  step 6  */
        if (isMapped) {
            if (desc.isAccessorDescriptor()) {
                map.delete(propertyKey);
            } else {
                if (desc.hasValue()) {
                    // Put(realm(), map, propertyKey, desc.getValue(), false);
                    map.put(propertyKey, desc.getValue());
                }
                if (desc.hasWritable() && !desc.isWritable()) {
                    map.delete(propertyKey);
                }
            }
        }
        return true;
    }

    /**
     * [[Delete]]
     */
    @Override
    public boolean delete(ExecutionContext cx, String propertyKey) {
        /*  step 1  */
        ParameterMap map = this.parameterMap;
        /*  [[ParameterMap]] not present  */
        if (map == null) {
            return super.delete(cx, propertyKey);
        }
        /*  step 2  */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]]) (Bug 1412)
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /*  step 3  */
        boolean result = super.delete(cx, propertyKey);
        if (result && isMapped) {
            map.delete(propertyKey);
        }
        return result;
    }
}