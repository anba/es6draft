/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>9.4.6 Module Exotic Objects
 * </ul>
 */
public final class ExoticModule extends OrdinaryObject {
    /** [[ModuleEnvironment]] */
    private final LexicalEnvironment<?> moduleEnvironment;
    /** [[Exports]] */
    private final List<String> exports;

    public ExoticModule(Realm realm, LexicalEnvironment<?> moduleEnvironment, List<String> exports) {
        super(realm);
        this.moduleEnvironment = moduleEnvironment;
        this.exports = exports;
    }

    public LexicalEnvironment<?> getModuleEnvironment() {
        return moduleEnvironment;
    }

    public List<String> getExports() {
        return exports;
    }

    /** 9.4.6.1 [[GetPrototypeOf]] ( ) */
    @Override
    public ScriptObject getPrototypeOf(ExecutionContext cx) {
        return null;
    }

    /** 9.4.6.2 [[SetPrototypeOf]] (V) */
    @Override
    public boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype) {
        return false;
    }

    /** 9.4.6.3 [[IsExtensible]] ( ) */
    @Override
    public boolean isExtensible(ExecutionContext cx) {
        return false;
    }

    /** 9.4.6.4 [[PreventExtensions]] ( ) */
    @Override
    public boolean preventExtensions(ExecutionContext cx) {
        return true;
    }

    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        throw newTypeError(cx, Messages.Key.InternalError);// TODO: error message
    }

    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        throw newTypeError(cx, Messages.Key.InternalError);// TODO: error message
    }

    /** 9.4.6.5 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        throw newTypeError(cx, Messages.Key.InternalError);// TODO: error message
    }

    /** 9.4.6.5 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        throw newTypeError(cx, Messages.Key.InternalError);// TODO: error message
    }

    /** 9.4.6.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        return false;
    }

    /** 9.4.6.6 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey,
            PropertyDescriptor desc) {
        return false;
    }

    /** 9.4.6.7 [[HasProperty]] (P) */
    @Override
    public boolean hasProperty(ExecutionContext cx, String propertyKey) {
        /* steps 1-3 */
        return exports.contains(propertyKey);
    }

    /** 9.4.6.7 [[HasProperty]] (P) */
    @Override
    public boolean hasProperty(ExecutionContext cx, Symbol propertyKey) {
        return false;
    }

    /** 9.4.6.8 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        if (!exports.contains(propertyKey)) {
            return UNDEFINED;
        }
        /* steps 4-5 */
        return moduleEnvironment.getEnvRec().getBindingValue(propertyKey, true);
    }

    /** 9.4.6.8 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, Symbol propertyKey, Object receiver) {
        return UNDEFINED;
    }

    /** 9.4.6.9 [[Set]] ( P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        return false;
    }

    /** 9.4.6.9 [[Set]] ( P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver) {
        return false;
    }

    /** 9.4.6.10 [[Delete]] (P) */
    @Override
    public boolean delete(ExecutionContext cx, String propertyKey) {
        /* step 1 (not applicable) */
        /* steps 2-4 */
        return !exports.contains(propertyKey);
    }

    /** 9.4.6.10 [[Delete]] (P) */
    @Override
    public boolean delete(ExecutionContext cx, Symbol propertyKey) {
        return true;
    }

    /** 9.4.6.11 [[Enumerate]] () */
    @Override
    protected List<String> enumerateKeys(ExecutionContext cx) {
        return exports;
    }

    @Override
    protected boolean isEnumerableOwnProperty(String propertyKey) {
        assert exports.contains(propertyKey) : String.format("'%s' is not an exported binding",
                propertyKey);
        return true;
    }

    /** 9.4.6.12 [[OwnPropertyKeys]] ( ) */
    @Override
    @SuppressWarnings("unchecked")
    protected List<Object> enumerateOwnKeys(ExecutionContext cx) {
        return (List<Object>) (List<?>) exports;
    }

    /**
     * 9.4.6.13 ModuleObjectCreate (environment, exports)
     *
     * @param cx
     *            the execution context
     * @param environment
     *            the module environment
     * @param exports
     *            the exported bindings
     * @return the new module object
     */
    public static ExoticModule ModuleObjectCreate(ExecutionContext cx,
            LexicalEnvironment<?> environment, List<String> exports) {
        /* steps 1-2 (not applicable) */
        /* steps 3-7 */
        return new ExoticModule(cx.getRealm(), environment, exports);
    }
}
