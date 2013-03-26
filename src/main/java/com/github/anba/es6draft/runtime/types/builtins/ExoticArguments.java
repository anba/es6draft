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

import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

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
public class ExoticArguments extends OrdinaryObject implements ScriptObject {
    /** [[ParameterMap]] */
    private ParameterMap parameterMap = null;

    private static class ParameterMap {
        private LexicalEnvironment env;
        private int length;
        private String[] args;

        ParameterMap(LexicalEnvironment env, int length) {
            this.env = env;
            this.length = length;
            this.args = new String[length];
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
            args[propertyKey] = name;
        }

        Object get(String propertyKey) {
            int index = toArgumentIndex(propertyKey);
            if (index >= 0 && index < length) {
                String name = args[index];
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
                String name = args[index];
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
                return args[index] != null;
            }
            return false;
        }

        boolean delete(String propertyKey) {
            int index = toArgumentIndex(propertyKey);
            if (index >= 0 && index < length) {
                args[index] = null;
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
    public static ExoticArguments InstantiateArgumentsObject(Realm realm, Object[] args) {
        /* [10.6] step 1 */
        int len = args.length;
        /* [10.6] step 2-3 */
        ExoticArguments obj = new ExoticArguments(realm);
        obj.setPrototype(realm, realm.getIntrinsic(Intrinsics.ObjectPrototype));
        /* [10.6] step 4 */
        obj.defineOwnProperty(realm, "length", new PropertyDescriptor(len, true, false, true));
        /* [10.6] step 5-6 */
        for (int index = 0; index < len; ++index) {
            Object val = args[index];
            obj.defineOwnProperty(realm, ToString(index), new PropertyDescriptor(val, true, true,
                    true));
        }
        return obj;
    }

    /**
     * [10.6 Arguments Object] CompleteStrictArgumentsObject
     */
    public static void CompleteStrictArgumentsObject(Realm realm, ExoticArguments obj) {
        /* [10.6] step 1-2 */
        AddRestrictedFunctionProperties(realm, obj);
    }

    /**
     * [13.6 Creating Function Objects and Constructors] AddRestrictedFunctionProperties
     */
    private static void AddRestrictedFunctionProperties(Realm realm, ExoticArguments obj) {
        /*  step 1  */
        Callable thrower = realm.getThrowTypeError();
        /*  step 2  */
        obj.defineOwnProperty(realm, "caller", new PropertyDescriptor(thrower, thrower, false,
                false));
        /*  step 3  */
        // FIXME: spec bug ("arguments" per spec!) (Bug 1158)
        obj.defineOwnProperty(realm, "callee", new PropertyDescriptor(thrower, thrower, false,
                false));
    }

    /**
     * [10.6 Arguments Object] CompleteMappedArgumentsObject
     */
    public static void CompleteMappedArgumentsObject(Realm realm, ExoticArguments obj,
            FunctionObject func, String[] formals, LexicalEnvironment env) {
        // added ToInt32()
        int len = ToInt32(realm, Get(realm, obj, "length"));
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
        obj.defineOwnProperty(realm, "callee", new PropertyDescriptor(func, true, false, true));
    }

    /**
     * [[Set]]
     */
    @Override
    public boolean set(Realm realm, String propertyKey, Object value, Object receiver) {
        // FIXME: spec bug (not overriden in spec -> 10.6-10-c-ii-2) (bug 1160)
        return super.set(realm, propertyKey, value, receiver);
    }

    /**
     * [[Get]]
     */
    @Override
    public Object get(Realm realm, String propertyKey, Object accessorThisValue) {
        /*  step 1-2  */
        ParameterMap map = this.parameterMap;
        /*  [[ParameterMap]] not present  */
        if (map == null) {
            return super.get(realm, propertyKey, accessorThisValue);
        }
        /*  step 3  */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]])
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /*  step 4  */
        if (!isMapped) {
            Object v = super.get(realm, propertyKey, accessorThisValue);
            if ("caller".equals(propertyKey) && isStrictFunction(v)) {
                throw throwTypeError(realm, Messages.Key.StrictModePoisonPill);
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
    public Property getOwnProperty(Realm realm, String propertyKey) {
        /*  step 1  */
        Property desc = super.getOwnProperty(realm, propertyKey);
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
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]])
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
    public boolean defineOwnProperty(Realm realm, String propertyKey, PropertyDescriptor desc) {
        /*  step 1  */
        ParameterMap map = this.parameterMap;
        /*  [[ParameterMap]] not present  */
        if (map == null) {
            return super.defineOwnProperty(realm, propertyKey, desc);
        }
        /*  step 2  */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]])
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /*  step 3-4  */
        boolean allowed = super.defineOwnProperty(realm, propertyKey, desc);
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
    public boolean delete(Realm realm, String propertyKey) {
        /*  step 1  */
        ParameterMap map = this.parameterMap;
        /*  [[ParameterMap]] not present  */
        if (map == null) {
            return super.delete(realm, propertyKey);
        }
        /*  step 2  */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]])
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /*  step 3  */
        boolean result = super.delete(realm, propertyKey);
        if (result && isMapped) {
            map.delete(propertyKey);
        }
        return result;
    }
}