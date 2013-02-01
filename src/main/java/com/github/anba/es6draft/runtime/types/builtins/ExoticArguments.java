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
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.HashSet;
import java.util.Set;

import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Function;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.Scriptable;

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
public class ExoticArguments extends OrdinaryObject implements Scriptable {
    // [[ParameterMap]]
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
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinArguments;
    }

    /**
     * [10.6 Arguments Object] InstantiateArgumentsObject
     */
    public static ExoticArguments InstantiateArgumentsObject(Realm realm, Object[] args) {
        /* [10.6] step 1 */
        int len = args.length;
        /* [10.6] step 2-3 */
        ExoticArguments obj = new ExoticArguments(realm);
        obj.setPrototype(realm.getIntrinsic(Intrinsics.ObjectPrototype));
        /* [10.6] step 4 */
        obj.defineOwnProperty("length", new PropertyDescriptor(len, true, false, true));
        /* [10.6] step 5-6 */
        for (int index = len - 1; index >= 0; --index) {
            Object val = args[index];
            obj.defineOwnProperty(ToString(index), new PropertyDescriptor(val, true, true, true));
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
        obj.defineOwnProperty("caller", new PropertyDescriptor(thrower, thrower, false, false));
        /*  step 3  */
        // FIXME: spec bug ("arguments" per spec!) (Bug 1158)
        obj.defineOwnProperty("callee", new PropertyDescriptor(thrower, thrower, false, false));
    }

    /**
     * [10.6 Arguments Object] CompleteMappedArgumentsObject
     */
    public static void CompleteMappedArgumentsObject(Realm realm, ExoticArguments obj,
            Function func, RuntimeInfo.FormalParameterList formals, LexicalEnvironment env) {
        // added ToInt32()
        int len = ToInt32(realm, Get(obj, "length"));
        Set<String> mappedNames = new HashSet<>();
        boolean hasMapped = false;
        int numberOfNonRestFormals = formals.numberOfParameters();
        // Scriptable map = ObjectCreate(realm);
        ParameterMap map = new ParameterMap(env, len);
        // FIXME: spec bug duplicate arguments vs mapped arguments (bug 1240)
        for (int index = numberOfNonRestFormals - 1; index >= 0; --index) {
            RuntimeInfo.FormalParameter param = formals.getParameter(index);
            if (param.isBindingIdentifier()) {
                String name = param.boundNames()[0];
                if (!mappedNames.contains(name)) {
                    mappedNames.add(name);
                    if (index < len) {
                        hasMapped = true;
                        map.defineOwnProperty(index, name);
                    }
                }
            }
        }
        if (hasMapped) {
            obj.parameterMap = map;
        }
        /*  step 9  */
        obj.defineOwnProperty("callee", new PropertyDescriptor(func, true, false, true));
    }

    /**
     * [10.6 Arguments Object] CreateStrictArgumentsObject
     */
    @SuppressWarnings("unused")
    @Deprecated
    private static Scriptable CreateStrictArgumentsObject(Realm realm, Object[] args) {
        /*  step 1  */
        int len = args.length;
        /*  step 2  */
        ExoticArguments obj = InstantiateArgumentsObject(realm, len);
        /*  step 3-4  */
        for (int index = len - 1; index >= 0; --index) {
            Object val = args[index];
            obj.defineOwnProperty(ToString(index), new PropertyDescriptor(val, true, true, true));
        }
        /*  step 5  */
        Callable thrower = realm.getThrowTypeError();
        /*  step 6  */
        obj.defineOwnProperty("caller", new PropertyDescriptor(thrower, thrower, false, false));
        /*  step 7  */
        obj.defineOwnProperty("callee", new PropertyDescriptor(thrower, thrower, false, false));
        /*  step 8  */
        return obj;
    }

    /**
     * [10.6 Arguments Object] CreateMappedArgumentsObject
     */
    @SuppressWarnings("unused")
    @Deprecated
    private static Scriptable CreateMappedArgumentsObject(Realm realm, Function func,
            String[] names, LexicalEnvironment env, Object[] args) {
        /*  step 1  */
        int len = args.length;
        /*  step 2  */
        ExoticArguments obj = InstantiateArgumentsObject(realm, len);
        /*  step 3-4  */
        Scriptable map = ObjectCreate(realm);
        /*  step 5  */
        Set<String> mappedNames = new HashSet<>();
        /*  step 6-7  */
        for (int index = len - 1; index >= 0; --index) {
            /*  step 7a-b  */
            Object val = args[index];
            obj.defineOwnProperty(ToString(index), new PropertyDescriptor(val, true, true, true));
            /*  step 7c  */
            if (index < names.length) {
                String name = names[index];
                if (!mappedNames.contains(name)) {
                    mappedNames.add(name);
                    Callable g = MakeArgGetter(name, env);
                    Callable p = MakeArgSetter(name, env);
                    map.defineOwnProperty(ToString(index),
                            new PropertyDescriptor(g, p, false, true));
                }
            }
        }
        /*  step 8  */
        if (!mappedNames.isEmpty()) {
            // obj.parameterMap = map;
        }
        /*  step 9  */
        obj.defineOwnProperty("callee", new PropertyDescriptor(func, true, true, true));
        /*  step 10  */
        return obj;
    }

    /**
     * [10.6 Arguments Object] InstantiateArgumentsObject
     */
    @Deprecated
    private static ExoticArguments InstantiateArgumentsObject(Realm realm, int len) {
        ExoticArguments obj = new ExoticArguments(realm);
        obj.setPrototype(realm.getIntrinsic(Intrinsics.ObjectPrototype));
        obj.defineOwnProperty("length", new PropertyDescriptor(len, true, false, true));
        return obj;
    }

    /**
     * [10.6 Arguments Object] MakeArgGetter
     */
    private static Callable MakeArgGetter(final String name, final LexicalEnvironment env) {
        return new Callable() {
            @Override
            public String toSource() {
                return "function F(){ /* native code */ }";
            }

            @Override
            public Object call(Object thisValue, Object... args) {
                // return <name>;

                // 12.9 The return Statement, Runtime Semantics: Evaluation
                Reference ref = LexicalEnvironment.getIdentifierReference(env, name, true);
                // passing 'null' is safe here
                Object value = ref.GetValue(null);

                return value;
            }
        };
    }

    /**
     * [10.6 Arguments Object] MakeArgSetter
     */
    private static Callable MakeArgSetter(final String name, final LexicalEnvironment env) {
        return new Callable() {
            @Override
            public String toSource() {
                return "function F(){ /* native code */ }";
            }

            @Override
            public Object call(Object thisValue, Object... args) {
                // <name> = <param>;

                // 11.13 Assignment Operators, Runtime Semantics: Evaluation
                Reference ref = LexicalEnvironment.getIdentifierReference(env, name, true);
                Object param = args[0];
                // passing 'null' is safe here
                ref.PutValue(param, null);

                return UNDEFINED;
            }
        };
    }

    /**
     * [[Set]]
     */
    @Override
    public boolean set(String propertyKey, Object value, Object receiver) {
        // FIXME: spec bug (not overriden in spec -> 10.6-10-c-ii-2) (bug 1160)
        return super.set(propertyKey, value, receiver);
    }

    /**
     * [[Get]]
     */
    @Override
    public Object get(String propertyKey, Object accessorThisValue) {
        /*  step 1-2  */
        ParameterMap map = this.parameterMap;
        /*  [[ParameterMap]] not present  */
        if (map == null) {
            return super.get(propertyKey, accessorThisValue);
        }
        /*  step 3  */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]])
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /*  step 4  */
        if (!isMapped) {
            Object v = super.get(propertyKey, accessorThisValue);
            if ("caller".equals(propertyKey) && v instanceof Function && ((Function) v).isStrict()) {
                throw throwTypeError(realm(), "poison pill caller");
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
    public Property getOwnProperty(String propertyKey) {
        /*  step 1  */
        Property desc = super.getOwnProperty(propertyKey);
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
    public boolean defineOwnProperty(String propertyKey, PropertyDescriptor desc) {
        /*  step 1  */
        ParameterMap map = this.parameterMap;
        /*  [[ParameterMap]] not present  */
        if (map == null) {
            return super.defineOwnProperty(propertyKey, desc);
        }
        /*  step 2  */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]])
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /*  step 3-4  */
        boolean allowed = super.defineOwnProperty(propertyKey, desc);
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
    public boolean delete(String propertyKey) {
        /*  step 1  */
        ParameterMap map = this.parameterMap;
        /*  [[ParameterMap]] not present  */
        if (map == null) {
            return super.delete(propertyKey);
        }
        /*  step 2  */
        // FIXME: spec issue ([[HasOwnProperty]] instead of [[GetOwnProperty]])
        // PropertyDescriptor isMapped = map.getOwnProperty(propertyKey);
        boolean isMapped = map.hasOwnProperty(propertyKey);
        /*  step 3  */
        boolean result = super.delete(propertyKey);
        if (result && isMapped) {
            map.delete(propertyKey);
        }
        return result;
    }
}