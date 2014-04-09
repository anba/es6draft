/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInt32;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.isStrictFunction;

import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>9.4.4 Exotic Arguments Objects
 * </ul>
 */
public final class ExoticArguments extends OrdinaryObject {
    /** [[ParameterMap]] */
    private ParameterMap parameterMap = null;

    public ExoticArguments(Realm realm) {
        super(realm);
    }

    /**
     * [[ParameterMap]]
     *
     * @return the parameter map
     */
    ParameterMap getParameterMap() {
        return parameterMap;
    }

    /**
     * Creates a mapped {@link ExoticArguments} object
     * <p>
     * [Called from generated code]
     * 
     * @param cx
     *            the execution context
     * @param func
     *            the function callee
     * @param args
     *            the function arguments
     * @param formals
     *            the formal parameter names
     * @param env
     *            the current lexical environment
     * @return the mapped arguments object
     */
    public static ExoticArguments CreateMappedArgumentsObject(ExecutionContext cx,
            FunctionObject func, Object[] args, String[] formals,
            LexicalEnvironment<? extends DeclarativeEnvironmentRecord> env) {
        ExoticArguments arguments = InstantiateArgumentsObject(cx, args);
        CompleteMappedArgumentsObject(cx, arguments, func, formals, env);
        return arguments;
    }

    /**
     * Creates a strict {@link ExoticArguments} object
     * <p>
     * [Called from generated code]
     * 
     * @param cx
     *            the execution context
     * @param args
     *            the function arguments
     * @return the strict mode arguments object
     */
    public static ExoticArguments CreateStrictArgumentsObject(ExecutionContext cx, Object[] args) {
        ExoticArguments arguments = InstantiateArgumentsObject(cx, args);
        CompleteStrictArgumentsObject(cx, arguments);
        return arguments;
    }

    /**
     * [9.4.4.1 Arguments Object] InstantiateArgumentsObject
     * 
     * @param cx
     *            the execution context
     * @param args
     *            the function arguments
     * @return the new arguments object
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
     * 
     * @param cx
     *            the execution context
     * @param obj
     *            the arguments object
     */
    public static void CompleteStrictArgumentsObject(ExecutionContext cx, ExoticArguments obj) {
        /* steps 1-2 */
        AddRestrictedArgumentsProperties(cx, obj);
    }

    /**
     * [9.4.4.1 Arguments Object] AddRestrictedArgumentsProperties
     * 
     * @param cx
     *            the execution context
     * @param obj
     *            the arguments object
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
     * 
     * @param cx
     *            the execution context
     * @param obj
     *            the arguments object
     * @param func
     *            the function callee
     * @param formals
     *            the formal parameter names
     * @param env
     *            the current lexical environment
     */
    public static void CompleteMappedArgumentsObject(ExecutionContext cx, ExoticArguments obj,
            FunctionObject func, String[] formals,
            LexicalEnvironment<? extends DeclarativeEnvironmentRecord> env) {
        // added ToInt32()
        int len = ToInt32(cx, Get(cx, obj, "length"));
        obj.parameterMap = ParameterMap.create(len, formals, env);
        /* step 9 */
        obj.defineOwnProperty(cx, "callee", new PropertyDescriptor(func, true, false, true));
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
        boolean isMapped = map.hasOwnProperty(propertyKey, false);
        /* step 4 */
        if (!isMapped) {
            // FIXME: spec bug (does not work as intended) (Bug 1413)
            Object v = super.get(cx, propertyKey, receiver);
            if ("caller".equals(propertyKey) && isStrictFunction(v)
                    && cx.getRealm().isEnabled(CompatibilityOption.FunctionPrototype)) {
                throw newTypeError(cx, Messages.Key.StrictModePoisonPill);
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
        /* step 2 */
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
        boolean isMapped = map.hasOwnProperty(propertyKey, false);
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