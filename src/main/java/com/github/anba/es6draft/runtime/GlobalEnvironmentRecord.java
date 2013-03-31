/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsExtensible;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>10 Executable Code and Execution Contexts</h1><br>
 * <h2>10.2 Lexical Environments</h2><br>
 * <h3>10.2.1 Environment Records</h3>
 * <ul>
 * <li>10.2.1.4 Global Environment Records
 * </ul>
 */
public final class GlobalEnvironmentRecord implements EnvironmentRecord {
    private final ExecutionContext cx;
    private final ScriptObject globalObject;
    private final ObjectEnvironmentRecord objectEnv;
    private final DeclarativeEnvironmentRecord declEnv;
    private List<String> varNames = new ArrayList<>();

    public GlobalEnvironmentRecord(ExecutionContext cx, ScriptObject globalObject) {
        this.cx = cx;
        this.globalObject = globalObject;
        objectEnv = new ObjectEnvironmentRecord(cx, globalObject, false);
        declEnv = new DeclarativeEnvironmentRecord(cx);
    }

    /**
     * 10.2.1.4.1 HasBinding(N)
     */
    @Override
    public boolean hasBinding(String name) {
        if (declEnv.hasBinding(name)) {
            return true;
        }
        return objectEnv.hasBinding(name);
    }

    /**
     * 10.2.1.4.2 CreateMutableBinding (N, D)
     */
    @Override
    public void createMutableBinding(String name, boolean deletable) {
        declEnv.createMutableBinding(name, deletable);
    }

    /**
     * 10.2.1.4.3 CreateImmutableBinding (N)
     */
    @Override
    public void createImmutableBinding(String name) {
        declEnv.createImmutableBinding(name);
    }

    /**
     * 10.2.1.4.4 InitializeBinding (N,V)
     */
    @Override
    public void initializeBinding(String name, Object value) {
        if (declEnv.hasBinding(name)) {
            declEnv.initializeBinding(name, value);
            return;
        }
        // TODO: if-check necessary?
        if (objectEnv.hasBinding(name)) {
            objectEnv.initializeBinding(name, value);
        }
    }

    /**
     * 10.2.1.4.5 SetMutableBinding (N,V,S)
     */
    @Override
    public void setMutableBinding(String name, Object value, boolean strict) {
        if (declEnv.hasBinding(name)) {
            declEnv.setMutableBinding(name, value, strict);
            return;
        }
        objectEnv.setMutableBinding(name, value, strict);
    }

    /**
     * 10.2.1.4.6 GetBindingValue(N,S)
     */
    @Override
    public Object getBindingValue(String name, boolean strict) {
        if (declEnv.hasBinding(name)) {
            return declEnv.getBindingValue(name, strict);
        }
        return objectEnv.getBindingValue(name, strict);
    }

    /**
     * 10.2.1.4.7 DeleteBinding (N)
     */
    @Override
    public boolean deleteBinding(String name) {
        if (declEnv.hasBinding(name)) {
            return declEnv.deleteBinding(name);
        }
        if (objectEnv.hasBinding(name)) {
            // FIXME: spec bug (draft calls `declEnv.deleteBinding(name)`)
            boolean status = objectEnv.deleteBinding(name);
            if (status) {
                varNames.remove(name);
            }
            return status;
        }
        return true;
    }

    /**
     * 10.2.1.4.8 HasThisBinding ()
     */
    @Override
    public boolean hasThisBinding() {
        return true;
    }

    /**
     * 10.2.1.4.9 HasSuperBinding ()
     */
    @Override
    public boolean hasSuperBinding() {
        return false;
    }

    /**
     * 10.2.1.4.10 WithBaseObject()
     */
    @Override
    public ScriptObject withBaseObject() {
        return null;
    }

    /**
     * 10.2.1.4.11 GetThisBinding ()
     */
    @Override
    public Object getThisBinding() {
        return globalObject;
    }

    /**
     * 10.2.1.4.12 HasVarDeclaration (N)
     */
    public boolean hasVarDeclaration(String name) {
        return varNames.contains(name);
    }

    /**
     * 10.2.1.4.13 HasLexicalDeclaration (N)
     */
    public boolean hasLexicalDeclaration(String name) {
        return declEnv.hasBinding(name);
    }

    /**
     * 10.2.1.4.14 CanDeclareGlobalVar (N)
     */
    public boolean canDeclareGlobalVar(String name) {
        if (objectEnv.hasBinding(name)) {
            return true;
        }
        return IsExtensible(cx, globalObject);
    }

    /**
     * 10.2.1.4.15 CanDeclareGlobalFunction (N)
     */
    public boolean canDeclareGlobalFunction(String name) {
        boolean extensible = IsExtensible(cx, globalObject);
        if (!objectEnv.hasBinding(name)) {
            return extensible;
        }
        Property existingProp = globalObject.getOwnProperty(cx, name);
        if (existingProp == null) {
            return extensible;
        }
        if (existingProp.isConfigurable()) {
            return true;
        }
        if (existingProp.isDataDescriptor() && existingProp.isWritable()
                && existingProp.isEnumerable()) {
            return true;
        }
        return false;
    }

    /**
     * 10.2.1.4.16 CreateGlobalVarBinding (N, D)
     */
    public void createGlobalVarBinding(String name, boolean deletable) {
        if (!canDeclareGlobalVar(name)) {
            throw new IllegalStateException();
        }
        if (!objectEnv.hasBinding(name)) {
            objectEnv.createMutableBinding(name, deletable);
        }
        if (!varNames.contains(name)) {
            varNames.add(name);
        }
    }

    /**
     * 10.2.1.4.17 CreateGlobalFunctionBinding (N, V, D)
     */
    public void createGlobalFunctionBinding(String name, Object value, boolean deletable) {
        if (!canDeclareGlobalFunction(name)) {
            throw new IllegalStateException();
        }
        Property existingProp = globalObject.getOwnProperty(cx, name);
        if (existingProp == null || existingProp.isConfigurable()) {
            PropertyDescriptor desc = new PropertyDescriptor(value, true, true, deletable);
            globalObject.defineOwnProperty(cx, name, desc);
        } else {
            PropertyDescriptor desc = new PropertyDescriptor(value);
            globalObject.defineOwnProperty(cx, name, desc);
        }
        if (!varNames.contains(name)) {
            varNames.add(name);
        }
    }
}
