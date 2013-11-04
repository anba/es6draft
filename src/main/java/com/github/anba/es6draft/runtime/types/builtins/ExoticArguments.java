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
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>9.4.4 Exotic Arguments Objects
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
            return Strings.toIndex(p);
        }

        /**
         * Makes {@code arguments[propertyKey]} a mapped argument
         */
        void defineOwnProperty(int propertyKey, String name) {
            parameters[propertyKey] = name;
        }

        /**
         * Tests whether {@code propertyKey} is an array index for a mapped argument
         */
        boolean hasOwnProperty(String propertyKey) {
            int index = toArgumentIndex(propertyKey);
            if (index >= 0 && index < length) {
                return parameters[index] != null;
            }
            return false;
        }

        /**
         * See MakeArgGetter abstract operation
         */
        Object get(String propertyKey) {
            int index = toArgumentIndex(propertyKey);
            assert (index >= 0 && index < length && parameters[index] != null);
            String name = parameters[index];
            return env.getEnvRec().getBindingValue(name, true);
        }

        /**
         * See MakeArgSetter abstract operation
         */
        void put(String propertyKey, Object value) {
            int index = toArgumentIndex(propertyKey);
            assert (index >= 0 && index < length && parameters[index] != null);
            String name = parameters[index];
            env.getEnvRec().setMutableBinding(name, value, true);
        }

        /**
         * Makes {@code arguments[propertyKey]} no longer a mapped argument
         */
        boolean delete(String propertyKey) {
            int index = toArgumentIndex(propertyKey);
            assert (index >= 0 && index < length && parameters[index] != null);
            parameters[index] = null;
            return true;
        }
    }

    public ExoticArguments(Realm realm) {
        super(realm);
    }

    /**
     * [9.4.4.1 Arguments Object] InstantiateArgumentsObject
     */
    public static ExoticArguments InstantiateArgumentsObject(ExecutionContext cx, Object[] args) {
        /* step 1 */
        int len = args.length;
        /* steps 2-3 */
        ExoticArguments obj = new ExoticArguments(cx.getRealm());
        obj.setPrototype(cx.getIntrinsic(Intrinsics.ObjectPrototype));
        /* step 4 */
        obj.defineOwnProperty(cx, "length", new PropertyDescriptor(len, true, false, true));
        /* steps 5-6 */
        for (int index = 0; index < len; ++index) {
            Object val = args[index];
            obj.defineOwnProperty(cx, ToString(index),
                    new PropertyDescriptor(val, true, true, true));
        }
        return obj;
    }

    /**
     * [9.4.4.1 Arguments Object] CompleteStrictArgumentsObject
     */
    public static void CompleteStrictArgumentsObject(ExecutionContext cx, ExoticArguments obj) {
        /* steps 1-2 */
        AddRestrictedFunctionProperties(cx, obj);
    }

    /**
     * [13.6 Creating Function Objects and Constructors] AddRestrictedFunctionProperties
     */
    private static void AddRestrictedFunctionProperties(ExecutionContext cx, ExoticArguments obj) {
        /* step 1 */
        Callable thrower = cx.getRealm().getThrowTypeError();
        /* step 2 */
        obj.defineOwnProperty(cx, "caller", new PropertyDescriptor(thrower, thrower, false, false));
        /* step 3 */
        // FIXME: spec bug ("arguments" per spec!) (Bug 1158)
        obj.defineOwnProperty(cx, "callee", new PropertyDescriptor(thrower, thrower, false, false));
    }

    /**
     * [9.4.4.1 Arguments Object] CompleteMappedArgumentsObject
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
        /* step 9 */
        obj.defineOwnProperty(cx, "callee", new PropertyDescriptor(func, true, false, true));
    }

    /**
     * Creates a legacy {@link ExoticArguments} object
     */
    public static ExoticArguments CreateLegacyArguments(ExecutionContext cx,
            ExoticArguments arguments, FunctionObject func) {
        int length = ToInt32(cx, Get(cx, arguments, "length"));
        ExoticLegacyArguments obj = new ExoticLegacyArguments(cx.getRealm());
        obj.setPrototype(cx.getIntrinsic(Intrinsics.ObjectPrototype));
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

    /**
     * An {@link ExoticArguments} object with 'special' behaviour for legacy use
     */
    private static class ExoticLegacyArguments extends ExoticArguments {
        public ExoticLegacyArguments(Realm realm) {
            super(realm);
        }

        @Override
        public boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype) {
            // ignore attempts to change [[Prototype]]
            return true;
        }

        @Override
        public boolean preventExtensions(ExecutionContext cx) {
            // ignore attempts to change [[Extensible]]
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
     * Temporary helper method to work around bug 1866, called from generated code
     */
    public Object getArgument(ExecutionContext cx, String propertyKey) {
        if (hasOwnProperty(cx, propertyKey)) {
            return get(cx, propertyKey, this);
        }
        return UNDEFINED;
    }

    /**
     * [[Get]]
     */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        /* steps 1-2 */
        ParameterMap map = this.parameterMap;
        /* [[ParameterMap]] not present */
        if (map == null) {
            return super.get(cx, propertyKey, receiver);
        }
        /* step 3 */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]]) (Bug 1412)
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /* step 4 */
        if (!isMapped) {
            // FIXME: spec bug (does not work as intended) (Bug 1413)
            Object v = super.get(cx, propertyKey, receiver);
            if ("caller".equals(propertyKey) && isStrictFunction(v)) {
                throw throwTypeError(cx, Messages.Key.StrictModePoisonPill);
            }
            return v;
        }
        /* step 5 */
        // return Get(map, propertyKey);
        return map.get(propertyKey);
    }

    /**
     * [[GetOwnProperty]]
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        /* step 1 */
        Property desc = super.getOwnProperty(cx, propertyKey);
        /* step 3 */
        if (desc == null) {
            return desc;
        }
        /* step 3 */
        ParameterMap map = this.parameterMap;
        /* [[ParameterMap]] not present */
        if (map == null) {
            return desc;
        }
        /* step 4 */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]]) (Bug 1412)
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /* step 5 */
        if (isMapped) {
            // desc.setValue(Get(map, propertyKey));
            PropertyDescriptor d = desc.toPropertyDescriptor();
            d.setValue(map.get(propertyKey));
            desc = d.toProperty();
        }
        /* step 6 */
        return desc;
    }

    /**
     * [[DefineOwnProperty]]
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        /* step 1 */
        ParameterMap map = this.parameterMap;
        /* [[ParameterMap]] not present */
        if (map == null) {
            return super.defineOwnProperty(cx, propertyKey, desc);
        }
        /* step 2 */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]]) (Bug 1412)
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /* steps 3-4 */
        boolean allowed = super.defineOwnProperty(cx, propertyKey, desc);
        /* step 5 */
        if (!allowed) {
            return false;
        }
        /* step 6 */
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
        /* step 7 */
        return true;
    }

    /**
     * [[Delete]]
     */
    @Override
    public boolean delete(ExecutionContext cx, String propertyKey) {
        /* step 1 */
        ParameterMap map = this.parameterMap;
        /* [[ParameterMap]] not present */
        if (map == null) {
            return super.delete(cx, propertyKey);
        }
        /* step 2 */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]]) (Bug 1412)
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /* step 3 */
        boolean result = super.delete(cx, propertyKey);
        /* step 4 */
        if (result && isMapped) {
            map.delete(propertyKey);
        }
        /* step 5 */
        return result;
    }
}