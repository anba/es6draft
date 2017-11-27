/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Slots</h2>
 * <ul>
 * <li>9.4.4 Arguments Exotic Objects
 * </ul>
 */
public final class ArgumentsObject extends OrdinaryObject {
    /** [[ParameterMap]] */
    private final ParameterMap parameterMap;
    private boolean hasIndexedAccessors;

    /**
     * Constructs a new Arguments object.
     * 
     * @param realm
     *            the realm object
     * @param parameterMap
     *            the parameter map
     */
    public ArgumentsObject(Realm realm, ParameterMap parameterMap) {
        super(realm, realm.getIntrinsic(Intrinsics.ObjectPrototype));
        this.parameterMap = parameterMap;
    }

    /**
     * [[ParameterMap]]
     *
     * @return the parameter map
     */
    ParameterMap getParameterMap() {
        return parameterMap;
    }

    private boolean isMapped(long propertyKey) {
        return parameterMap != null && parameterMap.hasOwnProperty(propertyKey);
    }

    @Override
    public String className() {
        return "Arguments";
    }

    @Override
    public boolean hasIndexedAccessors() {
        return hasIndexedAccessors;
    }

    @Override
    Object getIndexed(long propertyKey) {
        if (isMapped(propertyKey)) {
            return parameterMap.get(propertyKey);
        }
        return super.getIndexed(propertyKey);
    }

    /**
     * 9.4.4.1 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, long propertyKey) {
        /* step 1 (not applicable) */
        /* step 2 */
        Property desc = ordinaryGetOwnProperty(propertyKey);
        /* step 3 */
        if (desc == null) {
            return desc;
        }
        /* step 4 */
        ParameterMap map = this.parameterMap;
        /* step 5 */
        boolean isMapped = map != null && map.hasOwnProperty(propertyKey);
        /* step 6 */
        if (isMapped) {
            // FIXME: spec issue - maybe add assertion: IsDataDescriptor(desc)?
            assert desc.isDataDescriptor();
            PropertyDescriptor d = desc.toPropertyDescriptor();
            d.setValue(map.get(propertyKey));
            desc = d.toProperty();
        }
        /* step 7 */
        return desc;
    }

    /**
     * 9.4.4.2 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        /* step 1 (not applicable) */
        /* step 2 */
        ParameterMap map = this.parameterMap;
        /* step 3 */
        boolean isMapped = map != null && map.hasOwnProperty(propertyKey);
        /* step 4 */
        PropertyDescriptor newArgDesc = desc;
        /* step 5 */
        if (isMapped && desc.isDataDescriptor()) {
            /* step 5.a */
            if (!desc.hasValue() && desc.hasWritable() && !desc.isWritable()) {
                newArgDesc = desc.clone();
                newArgDesc.setValue(map.get(propertyKey));
            }
        }
        /* step 6 */
        boolean allowed = ordinaryDefineOwnProperty(cx, propertyKey, newArgDesc);
        /* step 7 */
        if (!allowed) {
            return false;
        }
        /* step 8 */
        if (isMapped) {
            if (desc.isAccessorDescriptor()) {
                map.delete(propertyKey);
            } else {
                if (desc.hasValue()) {
                    map.put(propertyKey, desc.getValue());
                }
                if (desc.hasWritable() && !desc.isWritable()) {
                    map.delete(propertyKey);
                }
            }
        }
        hasIndexedAccessors |= desc.isAccessorDescriptor();
        /* step 9 */
        return true;
    }

    /**
     * 9.4.4.3 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(ExecutionContext cx, long propertyKey, Object receiver) {
        /* step 1 (not applicable) */
        /* step 2 */
        ParameterMap map = this.parameterMap;
        /* step 3 */
        boolean isMapped = map != null && map.hasOwnProperty(propertyKey);
        /* step 4 */
        if (!isMapped) {
            return ordinaryGet(cx, propertyKey, receiver);
        }
        /* step 5 */
        return map.get(propertyKey);
    }

    /**
     * 9.4.4.4 [[Set]] ( P, V, Receiver)
     */
    @Override
    public boolean set(ExecutionContext cx, long propertyKey, Object value, Object receiver) {
        /* step 1 (not applicable) */
        /* step 3.a */
        ParameterMap map = this.parameterMap;
        /* steps 2-3 */
        boolean isMapped;
        if (this != receiver) {
            /* step 2 */
            isMapped = false;
        } else {
            /* step 3 */
            isMapped = map != null && map.hasOwnProperty(propertyKey);
        }
        /* step 4 */
        if (isMapped) {
            map.put(propertyKey, value);

            // Skip updating the underlying property, because it's not observable.
            return true;
        }
        /* step 5 */
        return ordinarySet(cx, propertyKey, value, receiver);
    }

    /**
     * 9.4.4.5 [[Delete]] (P)
     */
    @Override
    public boolean delete(ExecutionContext cx, long propertyKey) {
        /* step 1 (not applicable) */
        /* step 2 */
        ParameterMap map = this.parameterMap;
        /* step 3 */
        boolean isMapped = map != null && map.hasOwnProperty(propertyKey);
        /* step 4 */
        boolean result = ordinaryDelete(cx, propertyKey);
        /* step 5 */
        if (result && isMapped) {
            map.delete(propertyKey);
        }
        /* step 6 */
        return result;
    }

    /**
     * 9.4.4.6 CreateUnmappedArgumentsObject(argumentsList)
     * <p>
     * [Called from generated code]
     * 
     * @param cx
     *            the execution context
     * @param argumentsList
     *            the function arguments
     * @return the strict mode arguments object
     */
    public static ArgumentsObject CreateUnmappedArgumentsObject(ExecutionContext cx, Object[] argumentsList) {
        /* step 1 */
        int len = argumentsList.length;
        /* steps 2-3 */
        ArgumentsObject obj = new ArgumentsObject(cx.getRealm(), null);
        /* step 4 */
        obj.infallibleDefineOwnProperty("length", new Property(len, true, false, true));
        /* steps 5-6 */
        for (int index = 0; index < len; ++index) {
            obj.setIndexed(index, argumentsList[index]);
        }
        Callable thrower = cx.getRealm().getThrowTypeError();
        /* step 7 */
        obj.infallibleDefineOwnProperty(BuiltinSymbol.iterator.get(),
                new Property(cx.getIntrinsic(Intrinsics.ArrayProto_values), true, false, true));
        /* step 8 */
        obj.infallibleDefineOwnProperty("callee", new Property(thrower, thrower, false, false));
        /* step 9 */
        return obj;
    }

    /**
     * 9.4.4.7 CreateMappedArgumentsObject ( func, formals, argumentsList, env )
     * <p>
     * [Called from generated code]
     * 
     * @param cx
     *            the execution context
     * @param func
     *            the function callee
     * @param argumentsList
     *            the function arguments
     * @param env
     *            the current lexical environment
     * @return the mapped arguments object
     */
    public static ArgumentsObject CreateMappedArgumentsObject(ExecutionContext cx, FunctionObject func,
            Object[] argumentsList, LexicalEnvironment<? extends DeclarativeEnvironmentRecord> env) {
        ParameterMap map = ParameterMap.create(func, argumentsList.length, env);
        return CreateMappedArgumentsObject(cx, func, argumentsList, map);
    }

    /**
     * 9.4.4.7 CreateMappedArgumentsObject ( func, formals, argumentsList, env )
     * <p>
     * [Called from generated code]
     * 
     * @param cx
     *            the execution context
     * @param func
     *            the function callee
     * @param argumentsList
     *            the function arguments
     * @return the mapped arguments object
     */
    public static ArgumentsObject CreateMappedArgumentsObject(ExecutionContext cx, FunctionObject func,
            Object[] argumentsList) {
        return CreateMappedArgumentsObject(cx, func, argumentsList, (ParameterMap) null);
    }

    /**
     * 9.4.4.7 CreateMappedArgumentsObject ( func, formals, argumentsList, env )
     * <p>
     * [Called from generated code]
     * 
     * @param cx
     *            the execution context
     * @param func
     *            the function callee
     * @param argumentsList
     *            the function arguments
     * @param map
     *            the parameter map
     * @return the mapped arguments object
     */
    private static ArgumentsObject CreateMappedArgumentsObject(ExecutionContext cx, FunctionObject func,
            Object[] argumentsList, ParameterMap map) {
        /* step 1 (not applicable) */
        /* step 2 */
        int len = argumentsList.length;
        /* steps 3-13 */
        ArgumentsObject obj = new ArgumentsObject(cx.getRealm(), map);
        /* steps 14-15 (not applicable) */
        /* steps 16-17 */
        for (int index = 0; index < len; ++index) {
            obj.setIndexed(index, argumentsList[index]);
        }
        /* step 18 */
        obj.infallibleDefineOwnProperty("length", new Property(len, true, false, true));
        /* steps 19-21 (not applicable) */
        /* step 22 */
        obj.infallibleDefineOwnProperty(BuiltinSymbol.iterator.get(),
                new Property(cx.getIntrinsic(Intrinsics.ArrayProto_values), true, false, true));
        /* step 23 */
        obj.infallibleDefineOwnProperty("callee", new Property(func, true, false, true));
        /* step 24 */
        return obj;
    }
}
