/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsExtensible;

import java.util.HashSet;
import java.util.Set;

import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>8 Executable Code and Execution Contexts</h1><br>
 * <h2>8.1 Lexical Environments</h2><br>
 * <h3>8.1.1 Environment Records</h3>
 * <ul>
 * <li>8.1.1.4 Global Environment Records
 * </ul>
 */
public final class GlobalEnvironmentRecord implements EnvironmentRecord {
    private final ExecutionContext cx;
    private final ScriptObject globalObject;
    private final ObjectEnvironmentRecord objectEnv;
    private final DeclarativeEnvironmentRecord declEnv;
    private Set<String> varNames = new HashSet<>();

    public GlobalEnvironmentRecord(ExecutionContext cx, ScriptObject globalObject) {
        this.cx = cx;
        this.globalObject = globalObject;
        objectEnv = new ObjectEnvironmentRecord(cx, globalObject, false);
        declEnv = new DeclarativeEnvironmentRecord(cx);
    }

    @Override
    public String toString() {
        return String.format("%s: {%n  objectEnv=%s,%n  declEnv=%s%n}", getClass().getSimpleName(),
                objectEnv, declEnv);
    }

    /**
     * 8.1.1.4.1 HasBinding(N)
     */
    @Override
    public boolean hasBinding(String name) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (declEnv.hasBinding(name)) {
            return true;
        }
        /* step 4 (omitted) */
        /* step 5 */
        return objectEnv.hasBinding(name);
    }

    /**
     * 8.1.1.4.2 CreateMutableBinding (N, D)
     */
    @Override
    public void createMutableBinding(String name, boolean deletable) {
        /* steps 1-2 (omitted) */
        /* steps 3-4 */
        declEnv.createMutableBinding(name, deletable);
    }

    /**
     * 8.1.1.4.3 CreateImmutableBinding (N)
     */
    @Override
    public void createImmutableBinding(String name) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        declEnv.createImmutableBinding(name);
    }

    /**
     * 8.1.1.4.4 InitialiseBinding (N,V)
     */
    @Override
    public void initialiseBinding(String name, Object value) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (declEnv.hasBinding(name)) {
            declEnv.initialiseBinding(name, value);
            return;
        }
        /* step 4 */
        // TODO: assert?
        /* step 5 (omitted) */
        /* step 6 */
        objectEnv.initialiseBinding(name, value);
    }

    /**
     * 8.1.1.4.5 SetMutableBinding (N,V,S)
     */
    @Override
    public void setMutableBinding(String name, Object value, boolean strict) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (declEnv.hasBinding(name)) {
            declEnv.setMutableBinding(name, value, strict);
            return;
        }
        /* step 4 (omitted) */
        /* step 5 */
        objectEnv.setMutableBinding(name, value, strict);
    }

    /**
     * 8.1.1.4.6 GetBindingValue(N,S)
     */
    @Override
    public Object getBindingValue(String name, boolean strict) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (declEnv.hasBinding(name)) {
            return declEnv.getBindingValue(name, strict);
        }
        /* step 4 (omitted) */
        /* step 5 */
        return objectEnv.getBindingValue(name, strict);
    }

    /**
     * 8.1.1.4.7 DeleteBinding (N)
     */
    @Override
    public boolean deleteBinding(String name) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (declEnv.hasBinding(name)) {
            return declEnv.deleteBinding(name);
        }
        /* step 4 (omitted) */
        /* step 5 */
        if (objectEnv.hasBinding(name)) {
            boolean status = objectEnv.deleteBinding(name);
            if (status) {
                varNames.remove(name);
            }
            return status;
        }
        /* step 6 */
        return true;
    }

    /**
     * 8.1.1.4.8 HasThisBinding ()
     */
    @Override
    public boolean hasThisBinding() {
        /* step 1 */
        return true;
    }

    /**
     * 8.1.1.4.9 HasSuperBinding ()
     */
    @Override
    public boolean hasSuperBinding() {
        /* step 1 */
        return false;
    }

    /**
     * 8.1.1.4.10 WithBaseObject()
     */
    @Override
    public ScriptObject withBaseObject() {
        /* step 1 */
        return null;
    }

    /**
     * 8.1.1.4.11 GetThisBinding ()
     */
    @Override
    public Object getThisBinding() {
        /* steps 1-3 (omitted) */
        /* step 4 */
        return globalObject;
    }

    /**
     * 8.1.1.4.12 HasVarDeclaration (N)
     */
    public boolean hasVarDeclaration(String name) {
        /* steps 1-2 (omitted) */
        /* steps 3-4 */
        return varNames.contains(name);
    }

    /**
     * 8.1.1.4.13 HasLexicalDeclaration (N)
     */
    public boolean hasLexicalDeclaration(String name) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        return declEnv.hasBinding(name);
    }

    /**
     * 8.1.1.4.14 CanDeclareGlobalVar (N)
     */
    public boolean canDeclareGlobalVar(String name) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (objectEnv.hasBinding(name)) {
            return true;
        }
        /* step 4 (omitted) */
        /* steps 5-6 */
        return IsExtensible(cx, globalObject);
    }

    /**
     * 8.1.1.4.15 CanDeclareGlobalFunction (N)
     */
    public boolean canDeclareGlobalFunction(String name) {
        /* steps 1-3 (omitted) */
        /* steps 4-5 */
        boolean extensible = IsExtensible(cx, globalObject);
        /* step 6 */
        if (!objectEnv.hasBinding(name)) {
            return extensible;
        }
        /* step 7 */
        Property existingProp = globalObject.getOwnProperty(cx, name);
        /* step 8 */
        if (existingProp == null) {
            return extensible;
        }
        /* step 9 */
        if (existingProp.isConfigurable()) {
            return true;
        }
        /* step 10 */
        if (existingProp.isDataDescriptor() && existingProp.isWritable()
                && existingProp.isEnumerable()) {
            return true;
        }
        /* step 11 */
        return false;
    }

    /**
     * 8.1.1.4.16 CreateGlobalVarBinding (N, D)
     */
    public void createGlobalVarBinding(String name, boolean deletable) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (!objectEnv.hasBinding(name)) {
            objectEnv.createMutableBinding(name, deletable);
        }
        /* step 4 (omitted) */
        /* step 5 */
        if (!varNames.contains(name)) {
            varNames.add(name);
        }
        /* step 6 */
        return;
    }

    /**
     * 8.1.1.4.17 CreateGlobalFunctionBinding (N, V, D)
     */
    public void createGlobalFunctionBinding(String name, Object value, boolean deletable) {
        /* steps 1-3 (omitted) */
        /* step 4 */
        Property existingProp = globalObject.getOwnProperty(cx, name);
        /* steps 5-6 */
        PropertyDescriptor desc;
        if (existingProp == null || existingProp.isConfigurable()) {
            desc = new PropertyDescriptor(value, true, true, deletable);
        } else {
            desc = new PropertyDescriptor(value);
        }
        /* steps 7-8 */
        DefinePropertyOrThrow(cx, globalObject, name, desc);
        /* step 9 (omitted) */
        /* step 10 */
        if (!varNames.contains(name)) {
            varNames.add(name);
        }
        /* step 11 */
        return;
    }
}
