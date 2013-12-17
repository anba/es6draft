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
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.isStrictFunction;

import java.util.BitSet;

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
    // = `this instanceof ExoticLegacyArguments`
    private final boolean isLegacy;

    /** [[ParameterMap]] */
    private ParameterMap parameterMap = null;

    private static final class ParameterMap {
        private final LexicalEnvironment env;
        private final int length;
        private final String[] parameters;
        private final BitSet legacyUnmapped;

        ParameterMap(LexicalEnvironment env, int length) {
            this.env = env;
            this.length = length;
            this.parameters = new String[length];
            this.legacyUnmapped = new BitSet();
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
        boolean hasOwnProperty(String propertyKey, boolean isLegacy) {
            int index = toArgumentIndex(propertyKey);
            if (index >= 0 && index < length && !(isLegacy && legacyUnmapped.get(index))) {
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
            legacyUnmapped.set(index);
            String name = parameters[index];
            env.getEnvRec().setMutableBinding(name, value, true);
        }

        /**
         * Removes mapping for {@code arguments[propertyKey]}
         */
        boolean delete(String propertyKey) {
            int index = toArgumentIndex(propertyKey);
            assert (index >= 0 && index < length && parameters[index] != null);
            legacyUnmapped.set(index);
            parameters[index] = null;
            return true;
        }
    }

    public ExoticArguments(Realm realm, boolean isLegacy) {
        super(realm);
        this.isLegacy = isLegacy;
    }

    /**
     * Creates a mapped {@link ExoticArguments} object
     * <p>
     * [Called from generated code]
     */
    public static ExoticArguments CreateMappedArgumentsObject(ExecutionContext cx,
            FunctionObject func, Object[] args, String[] formals, LexicalEnvironment env) {
        ExoticArguments arguments = InstantiateArgumentsObject(cx, args);
        CompleteMappedArgumentsObject(cx, arguments, func, formals, env);
        return arguments;
    }

    /**
     * Creates a strict {@link ExoticArguments} object
     * <p>
     * [Called from generated code]
     */
    public static ExoticArguments CreateStrictArgumentsObject(ExecutionContext cx, Object[] args) {
        ExoticArguments arguments = InstantiateArgumentsObject(cx, args);
        CompleteStrictArgumentsObject(cx, arguments);
        return arguments;
    }

    /**
     * Creates a legacy {@link ExoticArguments} object
     * <p>
     * [Called from generated code]
     */
    public static ExoticArguments CreateLegacyArgumentsObject(ExecutionContext cx,
            FunctionObject func, Object[] args, String[] formals, LexicalEnvironment env) {
        ParameterMap map = createParameterMap(args.length, formals, env);
        return createLegacyArguments(cx, func, args, map);
    }

    /**
     * Creates a legacy {@link ExoticArguments} object
     * <p>
     * [Called from generated code]
     */
    public static ExoticArguments CreateLegacyArgumentsObject(ExecutionContext cx,
            FunctionObject func, Object[] args, ExoticArguments arguments) {
        return createLegacyArguments(cx, func, args, arguments.parameterMap);
    }

    /**
     * [9.4.4.1 Arguments Object] InstantiateArgumentsObject
     */
    public static ExoticArguments InstantiateArgumentsObject(ExecutionContext cx, Object[] args) {
        /* step 1 */
        int len = args.length;
        /* steps 2-3 */
        ExoticArguments obj = new ExoticArguments(cx.getRealm(), false);
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
        AddRestrictedArgumentsProperties(cx, obj);
    }

    /**
     * [9.4.4.1 Arguments Object] AddRestrictedArgumentsProperties
     */
    private static void AddRestrictedArgumentsProperties(ExecutionContext cx, ExoticArguments obj) {
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
        obj.parameterMap = createParameterMap(len, formals, env);
        /* step 9 */
        obj.defineOwnProperty(cx, "callee", new PropertyDescriptor(func, true, false, true));
    }

    private static ParameterMap createParameterMap(int len, String[] formals, LexicalEnvironment env) {
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
        return hasMapped ? map : null;
    }

    /**
     * Creates a legacy {@link ExoticArguments} object
     */
    private static ExoticArguments createLegacyArguments(ExecutionContext cx, FunctionObject func,
            Object[] args, ParameterMap map) {
        int length = args.length;
        ExoticLegacyArguments obj = new ExoticLegacyArguments(cx.getRealm());
        ((ExoticArguments) obj).parameterMap = map;
        obj.setPrototype(cx.getIntrinsic(Intrinsics.ObjectPrototype));
        obj.ordinaryDefineOwnProperty("length", new PropertyDescriptor(length, true, false, true));
        obj.ordinaryDefineOwnProperty("callee", new PropertyDescriptor(func, true, false, true));
        for (int index = 0; index < length; ++index) {
            String pk = ToString(index);
            Object value = args[index];
            obj.ordinaryDefineOwnProperty(pk, new PropertyDescriptor(value, true, true, true));
        }
        return obj;
    }

    /**
     * An {@link ExoticArguments} object with 'special' behaviour for legacy use
     */
    private static class ExoticLegacyArguments extends ExoticArguments {
        public ExoticLegacyArguments(Realm realm) {
            super(realm, true);
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
        boolean isMapped = map.hasOwnProperty(propertyKey, isLegacy);
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
        boolean isMapped = map.hasOwnProperty(propertyKey, isLegacy);
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
        boolean isMapped = map.hasOwnProperty(propertyKey, false);
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
        boolean isMapped = map.hasOwnProperty(propertyKey, false);
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