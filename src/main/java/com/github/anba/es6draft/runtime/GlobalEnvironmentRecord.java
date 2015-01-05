/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsExtensible;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import java.util.HashSet;
import java.util.Set;

import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Reference;
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
    private final ObjectEnvironmentRecord objectRec;
    private final DeclarativeEnvironmentRecord declRec;
    private final HashSet<String> varNames = new HashSet<>();

    public GlobalEnvironmentRecord(ExecutionContext cx, ScriptObject globalObject) {
        this.cx = cx;
        this.globalObject = globalObject;
        objectRec = new ObjectEnvironmentRecord(cx, globalObject, false);
        declRec = new DeclarativeEnvironmentRecord(cx);
    }

    @Override
    public String toString() {
        return String.format("%s: {%n\tobjectEnv=%s,%n\tdeclEnv=%s,%n\tvarNames=%s%n}", getClass()
                .getSimpleName(), objectRec, declRec, varNames);
    }

    @Override
    public Set<String> bindingNames() {
        HashSet<String> names = new HashSet<>();
        names.addAll(declRec.bindingNames());
        names.addAll(objectRec.bindingNames());
        return names;
    }

    @Override
    public Object getBindingValueOrNull(String name, boolean strict) {
        if (hasBinding(name)) {
            return getBindingValue(name, strict);
        }
        return null;
    }

    @Override
    public Reference<GlobalEnvironmentRecord, String> getReferenceOrNull(String name, boolean strict) {
        if (hasBinding(name)) {
            return new Reference.IdentifierReference<>(this, name, strict);
        }
        return null;
    }

    /**
     * 8.1.1.4.1 HasBinding(N)
     */
    @Override
    public boolean hasBinding(String name) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (declRec.hasBinding(name)) {
            return true;
        }
        /* step 4 (omitted) */
        /* step 5 */
        return objectRec.hasBinding(name);
    }

    /**
     * 8.1.1.4.2 CreateMutableBinding (N, D)
     */
    @Override
    public void createMutableBinding(String name, boolean deletable) {
        /* steps 1-2 (omitted) */
        /* step 3-4 */
        boolean alreadyThere = declRec.hasBinding(name);
        /* step 5 */
        if (alreadyThere) {
            throw newTypeError(cx, Messages.Key.VariableRedeclaration, name);
        }
        /* step 6 */
        declRec.createMutableBinding(name, deletable);
    }

    /**
     * 8.1.1.4.3 CreateImmutableBinding (N, S)
     */
    @Override
    public void createImmutableBinding(String name, boolean strict) {
        /* steps 1-2 (omitted) */
        /* step 3-4 */
        boolean alreadyThere = declRec.hasBinding(name);
        /* step 5 */
        if (alreadyThere) {
            throw newTypeError(cx, Messages.Key.VariableRedeclaration, name);
        }
        /* step 6 */
        declRec.createImmutableBinding(name, strict);
    }

    /**
     * 8.1.1.4.4 InitializeBinding (N,V)
     */
    @Override
    public void initializeBinding(String name, Object value) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (declRec.hasBinding(name)) {
            declRec.initializeBinding(name, value);
            return;
        }
        /* step 4 */
        // TODO: assert?
        /* step 5 (omitted) */
        /* step 6 */
        objectRec.initializeBinding(name, value);
    }

    /**
     * 8.1.1.4.5 SetMutableBinding (N,V,S)
     */
    @Override
    public void setMutableBinding(String name, Object value, boolean strict) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (declRec.hasBinding(name)) {
            declRec.setMutableBinding(name, value, strict);
            return;
        }
        /* step 4 (omitted) */
        /* step 5 */
        objectRec.setMutableBinding(name, value, strict);
    }

    /**
     * 8.1.1.4.6 GetBindingValue(N,S)
     */
    @Override
    public Object getBindingValue(String name, boolean strict) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (declRec.hasBinding(name)) {
            return declRec.getBindingValue(name, strict);
        }
        /* step 4 (omitted) */
        /* step 5 */
        return objectRec.getBindingValue(name, strict);
    }

    /**
     * 8.1.1.4.7 DeleteBinding (N)
     */
    @Override
    public boolean deleteBinding(String name) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (declRec.hasBinding(name)) {
            return declRec.deleteBinding(name);
        }
        /* step 4 (omitted) */
        /* step 5 */
        if (objectRec.hasBinding(name)) {
            boolean status = objectRec.deleteBinding(name);
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
    public ScriptObject getThisBinding() {
        /* steps 1-3 (omitted) */
        /* step 4 */
        return globalObject;
    }

    /**
     * 8.1.1.4.12 HasVarDeclaration (N)
     * 
     * @param name
     *            the binding name
     * @return {@code true} if the variable binding is present
     */
    public boolean hasVarDeclaration(String name) {
        /* steps 1-2 (omitted) */
        /* steps 3-4 */
        return varNames.contains(name);
    }

    /**
     * 8.1.1.4.13 HasLexicalDeclaration (N)
     * 
     * @param name
     *            the binding name
     * @return {@code true} if the lexical binding is present
     */
    public boolean hasLexicalDeclaration(String name) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        return declRec.hasBinding(name);
    }

    /**
     * 8.1.1.4.14 HasRestrictedGlobalProperty (N)
     * 
     * @param name
     *            the binding name
     * @return {@code true} if the global binding is present and non-configurable
     */
    public boolean hasRestrictedGlobalProperty(String name) {
        /* steps 1-3 (omitted) */
        /* steps 4-5 */
        Property existingProp = globalObject.getOwnProperty(cx, name);
        /* step 6 */
        if (existingProp == null) {
            return false;
        }
        /* step 7 */
        if (existingProp.isConfigurable()) {
            return false;
        }
        /* step 8 */
        return true;
    }

    /**
     * 8.1.1.4.15 CanDeclareGlobalVar (N)
     * 
     * @param name
     *            the binding name
     * @return {@code true} if the binding can be created
     */
    public boolean canDeclareGlobalVar(String name) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (objectRec.hasBinding(name)) {
            return true;
        }
        /* step 4 (omitted) */
        /* steps 5-6 */
        return IsExtensible(cx, globalObject);
    }

    /**
     * 8.1.1.4.16 CanDeclareGlobalFunction (N)
     * 
     * @param name
     *            the binding name
     * @return {@code true} if the binding can be created
     */
    public boolean canDeclareGlobalFunction(String name) {
        /* steps 1-3 (omitted) */
        /* steps 4-5 */
        boolean extensible = IsExtensible(cx, globalObject);
        /* step 6 */
        if (!objectRec.hasBinding(name)) {
            return extensible;
        }
        /* steps 7-8 */
        Property existingProp = globalObject.getOwnProperty(cx, name);
        /* step 9 */
        if (existingProp == null) {
            return extensible;
        }
        /* step 10 */
        if (existingProp.isConfigurable()) {
            return true;
        }
        /* step 11 */
        if (existingProp.isDataDescriptor() && existingProp.isWritable()
                && existingProp.isEnumerable()) {
            return true;
        }
        /* step 12 */
        return false;
    }

    /**
     * 8.1.1.4.17 CreateGlobalVarBinding (N, D)
     * 
     * @param name
     *            the binding name
     * @param deletable
     *            the deletable for the binding
     */
    public void createGlobalVarBinding(String name, boolean deletable) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        if (!objectRec.hasBinding(name)) {
            objectRec.createMutableBinding(name, deletable);
        }
        /* step 4 (omitted) */
        /* step 5 */
        varNames.add(name);
        /* step 6 */
        return;
    }

    /**
     * 8.1.1.4.18 CreateGlobalFunctionBinding (N, V, D)
     * 
     * @param name
     *            the binding name
     * @param value
     *            the function value
     * @param deletable
     *            the deletable for the binding
     */
    public void createGlobalFunctionBinding(String name, Object value, boolean deletable) {
        /* steps 1-3 (omitted) */
        /* steps 4-5 */
        Property existingProp = globalObject.getOwnProperty(cx, name);
        /* steps 6-7 */
        PropertyDescriptor desc;
        if (existingProp == null || existingProp.isConfigurable()) {
            desc = new PropertyDescriptor(value, true, true, deletable);
        } else {
            desc = new PropertyDescriptor(value);
        }
        /* steps 8-9 */
        DefinePropertyOrThrow(cx, globalObject, name, desc);
        /* step 10 (omitted) */
        /* step 11 */
        varNames.add(name);
        /* step 12 */
        return;
    }
}
