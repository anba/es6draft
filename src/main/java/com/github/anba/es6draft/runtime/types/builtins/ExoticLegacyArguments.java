/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;

/**
 * An {@link ExoticArguments} object with 'special' behaviour for legacy use
 */
public final class ExoticLegacyArguments extends OrdinaryObject {
    private final FunctionObject callee;
    private final Object[] arguments;
    private final ParameterMap parameterMap;

    public ExoticLegacyArguments(Realm realm, FunctionObject callee, Object[] arguments,
            ParameterMap map) {
        super(realm, null);
        this.callee = callee;
        this.arguments = arguments;
        this.parameterMap = map;
    }

    /**
     * Creates a legacy {@link ExoticArguments} object
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
     * @return the legacy arguments object
     */
    public static ExoticLegacyArguments CreateLegacyArgumentsObject(ExecutionContext cx,
            FunctionObject func, Object[] args, String[] formals,
            LexicalEnvironment<? extends DeclarativeEnvironmentRecord> env) {
        ParameterMap map = ParameterMap.create(args.length, formals, env);
        return createLegacyArguments(cx, func, args, map);
    }

    /**
     * Creates a legacy {@link ExoticArguments} object
     * <p>
     * [Called from generated code]
     * 
     * @param cx
     *            the execution context
     * @param func
     *            the function callee
     * @param args
     *            the function arguments
     * @param arguments
     *            the arguments object
     * @return the legacy arguments object
     */
    public static ExoticLegacyArguments CreateLegacyArgumentsObject(ExecutionContext cx,
            FunctionObject func, Object[] args, ExoticArguments arguments) {
        return createLegacyArguments(cx, func, args, arguments.getParameterMap());
    }

    /**
     * Creates a legacy {@link ExoticArguments} object
     * <p>
     * [Called from generated code]
     * 
     * @param cx
     *            the execution context
     * @param func
     *            the function callee
     * @param args
     *            the function arguments
     * @return the legacy arguments object
     */
    public static ExoticLegacyArguments CreateLegacyArgumentsObject(ExecutionContext cx,
            FunctionObject func, Object[] args) {
        return createLegacyArguments(cx, func, args, null);
    }

    /**
     * Creates a legacy {@link ExoticArguments} object
     * 
     * @param cx
     *            the execution context
     * @param func
     *            the function callee
     * @param args
     *            the function arguments
     * @param map
     *            the parameter map
     * @return the legacy arguments object
     */
    private static ExoticLegacyArguments createLegacyArguments(ExecutionContext cx,
            FunctionObject func, Object[] args, ParameterMap map) {
        ExoticLegacyArguments obj = new ExoticLegacyArguments(cx.getRealm(), func, args, map);
        obj.setPrototype(cx.getIntrinsic(Intrinsics.ObjectPrototype));
        return obj;
    }

    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        switch (propertyKey) {
        case "callee":
        case "length":
            return true;
        default:
            int index = ParameterMap.toArgumentIndex(propertyKey);
            return index >= 0 && index < arguments.length;
        }
    }

    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        // legacy arguments object has no own symbol-keyed properties
        return false;
    }

    @Override
    protected List<String> enumerateKeys(ExecutionContext cx) {
        ArrayList<String> keys = new ArrayList<>();
        addArgumentIndices(keys);
        return keys;
    }

    @Override
    protected List<Object> enumerateOwnKeys(ExecutionContext cx) {
        ArrayList<Object> keys = new ArrayList<>();
        addArgumentIndices(keys);
        return keys;
    }

    @Override
    protected boolean isEnumerableOwnProperty(ExecutionContext cx, String key) {
        int index = ParameterMap.toArgumentIndex(key);
        return index >= 0 && index < arguments.length;
    }

    private void addArgumentIndices(List<? super String> keys) {
        keys.add("length");
        keys.add("callee");
        for (int i = 0; i < arguments.length; ++i) {
            keys.add(Integer.toString(i));
        }
    }

    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        ParameterMap map = parameterMap;
        if (map == null || !map.hasOwnProperty(propertyKey, true)) {
            return super.get(cx, propertyKey, receiver);
        }
        return map.get(propertyKey);
    }

    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        switch (propertyKey) {
        case "callee":
            return new Property(callee, true, false, true);
        case "length":
            return new Property(arguments.length, true, false, true);
        }
        int index = ParameterMap.toArgumentIndex(propertyKey);
        if (index >= 0 && index < arguments.length) {
            ParameterMap map = parameterMap;
            if (map == null || !map.hasOwnProperty(propertyKey, true)) {
                return new Property(arguments[index], true, true, true);
            }
            return new Property(map.get(propertyKey), true, true, true);
        }
        return null;
    }

    @Override
    public Property getOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        // legacy arguments object has no own symbol-keyed properties
        return null;
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
