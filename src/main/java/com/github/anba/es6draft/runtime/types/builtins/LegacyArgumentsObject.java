/**
 * Copyright (c) 2012-2015 André Bargull
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
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;

/**
 * An {@link ArgumentsObject} object with 'special' behaviour for legacy use.
 */
public final class LegacyArgumentsObject extends OrdinaryObject {
    private final FunctionObject callee;
    private final Object[] arguments;
    private final ParameterMap parameterMap;

    /**
     * Constructs a new Legacy Arguments object.
     * 
     * @param realm
     *            the realm object
     * @param callee
     *            the callee function
     * @param arguments
     *            the function arguments
     * @param map
     *            the parameter map
     */
    LegacyArgumentsObject(Realm realm, FunctionObject callee, Object[] arguments, ParameterMap map) {
        super(realm, (Void) null);
        this.callee = callee;
        this.arguments = arguments;
        this.parameterMap = map;
    }

    /**
     * Creates a Legacy Arguments object
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
    public static LegacyArgumentsObject CreateLegacyArgumentsObject(ExecutionContext cx, FunctionObject func,
            Object[] args, String[] formals, LexicalEnvironment<? extends DeclarativeEnvironmentRecord> env) {
        ParameterMap map = ParameterMap.create(args.length, formals, env);
        return createLegacyArguments(cx, func, args, map);
    }

    /**
     * Creates a Legacy Arguments object
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
    public static LegacyArgumentsObject CreateLegacyArgumentsObject(ExecutionContext cx, FunctionObject func,
            Object[] args, ArgumentsObject arguments) {
        return createLegacyArguments(cx, func, args, arguments.getParameterMap());
    }

    /**
     * Creates a Legacy Arguments object
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
    public static LegacyArgumentsObject CreateLegacyArgumentsObject(ExecutionContext cx, FunctionObject func,
            Object[] args) {
        return createLegacyArguments(cx, func, args, null);
    }

    /**
     * Creates a Legacy Arguments object
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
    private static LegacyArgumentsObject createLegacyArguments(ExecutionContext cx, FunctionObject func, Object[] args,
            ParameterMap map) {
        LegacyArgumentsObject obj = new LegacyArgumentsObject(cx.getRealm(), func, args, map);
        obj.setPrototype(cx.getIntrinsic(Intrinsics.ObjectPrototype));
        return obj;
    }

    @Override
    public long getLength() {
        return arguments.length;
    }

    @Override
    public boolean hasSpecialIndexedProperties() {
        return true;
    }

    @Override
    protected boolean setPropertyValue(ExecutionContext cx, long propertyKey, Object value, Property current) {
        // this object is effectively unmodifiable
        return true;
    }

    @Override
    protected boolean setPropertyValue(ExecutionContext cx, String propertyKey, Object value, Property current) {
        // this object is effectively unmodifiable
        return true;
    }

    @Override
    protected boolean setPropertyValue(ExecutionContext cx, Symbol propertyKey, Object value, Property current) {
        // this object is effectively unmodifiable
        return true;
    }

    @Override
    protected boolean has(ExecutionContext cx, long propertyKey) {
        if (hasOwnProperty(cx, propertyKey)) {
            return true;
        }
        ScriptObject parent = getPrototypeOf(cx);
        return parent != null && parent.hasProperty(cx, propertyKey);
    }

    @Override
    protected boolean has(ExecutionContext cx, String propertyKey) {
        if (hasOwnProperty(cx, propertyKey)) {
            return true;
        }
        ScriptObject parent = getPrototypeOf(cx);
        return parent != null && parent.hasProperty(cx, propertyKey);
    }

    @Override
    protected boolean has(ExecutionContext cx, Symbol propertyKey) {
        if (hasOwnProperty(cx, propertyKey)) {
            return true;
        }
        ScriptObject parent = getPrototypeOf(cx);
        return parent != null && parent.hasProperty(cx, propertyKey);
    }

    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, long propertyKey) {
        int index = ParameterMap.toArgumentIndex(propertyKey);
        return 0 <= index && index < arguments.length;
    }

    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        switch (propertyKey) {
        case "callee":
        case "length":
            return true;
        default:
            return false;
        }
    }

    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        // legacy arguments object has no own symbol-keyed properties
        return false;
    }

    @Override
    protected List<String> getEnumerableKeys(ExecutionContext cx) {
        ArrayList<String> keys = new ArrayList<>(arguments.length + 2);
        addArgumentIndices(keys);
        return keys;
    }

    @Override
    protected List<Object> getOwnPropertyKeys(ExecutionContext cx) {
        ArrayList<Object> keys = new ArrayList<>(arguments.length + 2);
        addArgumentIndices(keys);
        return keys;
    }

    @Override
    protected Enumerability isEnumerableOwnProperty(String key) {
        int index = Strings.toArgumentIndex(key);
        return Enumerability.isEnumerable(0 <= index && index < arguments.length);
    }

    private void addArgumentIndices(List<? super String> keys) {
        for (int i = 0; i < arguments.length; ++i) {
            keys.add(Integer.toString(i));
        }
        keys.add("length");
        keys.add("callee");
    }

    @Override
    protected Object getValue(ExecutionContext cx, long propertyKey, Object receiver) {
        ParameterMap map = parameterMap;
        if (map == null || !map.hasOwnProperty(propertyKey, true)) {
            return super.getValue(cx, propertyKey, receiver);
        }
        return map.get(propertyKey);
    }

    @Override
    protected Property getProperty(ExecutionContext cx, long propertyKey) {
        int index = ParameterMap.toArgumentIndex(propertyKey);
        if (0 <= index && index < arguments.length) {
            ParameterMap map = parameterMap;
            if (map == null || !map.hasOwnProperty(propertyKey, true)) {
                return new Property(arguments[index], true, true, true);
            }
            return new Property(map.get(propertyKey), true, true, true);
        }
        return null;
    }

    @Override
    protected Property getProperty(ExecutionContext cx, String propertyKey) {
        switch (propertyKey) {
        case "callee":
            return new Property(callee, true, false, true);
        case "length":
            return new Property(arguments.length, true, false, true);
        }
        return null;
    }

    @Override
    protected Property getProperty(ExecutionContext cx, Symbol propertyKey) {
        // legacy arguments objects have no own symbol-keyed properties
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
    protected boolean deleteProperty(ExecutionContext cx, long propertyKey) {
        // this object is effectively unmodifiable
        return true;
    }

    @Override
    protected boolean deleteProperty(ExecutionContext cx, String propertyKey) {
        // this object is effectively unmodifiable
        return true;
    }

    @Override
    protected boolean deleteProperty(ExecutionContext cx, Symbol propertyKey) {
        // this object is effectively unmodifiable
        return true;
    }

    @Override
    protected boolean defineProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        // this object is effectively unmodifiable
        return true;
    }

    @Override
    protected boolean defineProperty(ExecutionContext cx, String propertyKey, PropertyDescriptor desc) {
        // this object is effectively unmodifiable
        return true;
    }

    @Override
    protected boolean defineProperty(ExecutionContext cx, Symbol propertyKey, PropertyDescriptor desc) {
        // this object is effectively unmodifiable
        return true;
    }
}
