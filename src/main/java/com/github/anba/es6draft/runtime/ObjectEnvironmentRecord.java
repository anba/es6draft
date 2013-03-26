/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;
import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.HasProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.Put;
import static com.github.anba.es6draft.runtime.internal.Errors.throwReferenceError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>10 Executable Code and Execution Contexts</h1><br>
 * <h2>10.2 Lexical Environments</h2><br>
 * <h3>10.2.1 Environment Records</h3>
 * <ul>
 * <li>10.2.1.2 Object Environment Records
 * </ul>
 */
public final class ObjectEnvironmentRecord implements EnvironmentRecord {
    private final Realm realm;
    private final ScriptObject bindings;
    private final boolean withEnvironment;

    public ObjectEnvironmentRecord(Realm realm, ScriptObject bindings, boolean withEnvironment) {
        this.realm = realm;
        this.bindings = bindings;
        this.withEnvironment = withEnvironment;
    }

    /**
     * 10.2.1.2.1 HasBinding(N)
     */
    @Override
    public boolean hasBinding(String name) {
        return HasProperty(realm, bindings, name);
    }

    /**
     * 10.2.1.2.2 CreateMutableBinding (N,D)
     */
    @Override
    public void createMutableBinding(String name, boolean deletable) {
        if (HasProperty(realm, bindings, name)) {
            throw new IllegalStateException();
        }
        PropertyDescriptor desc = new PropertyDescriptor(UNDEFINED, true, true, deletable);
        DefinePropertyOrThrow(realm, bindings, name, desc);
    }

    /**
     * 10.2.1.2.3 CreateImmutableBinding (N)
     */
    @Override
    public void createImmutableBinding(String name) {
        throw new IllegalStateException();
    }

    /**
     * 10.2.1.2.4 InitializeBinding (N,V)
     */
    @Override
    public void initializeBinding(String name, Object value) {
        assert value != null;
        // TODO: uninitialised binding in an object environment record?!
        // TODO: record the binding has been initialised
        setMutableBinding(name, value, false);
    }

    /**
     * 10.2.1.2.5 SetMutableBinding (N,V,S)
     */
    @Override
    public void setMutableBinding(String name, Object value, boolean strict) {
        Put(realm, bindings, name, value, strict);
    }

    /**
     * 10.2.1.2.6 GetBindingValue(N,S)
     */
    @Override
    public Object getBindingValue(String name, boolean strict) {
        boolean value = HasProperty(realm, bindings, name);
        if (!value) {
            if (!strict) {
                return UNDEFINED;
            }
            throw throwReferenceError(realm, Messages.Key.UnresolvableReference, name);
        }
        return Get(realm, bindings, name);
    }

    /**
     * 10.2.1.2.7 DeleteBinding (N)
     */
    @Override
    public boolean deleteBinding(String name) {
        return bindings.delete(realm, name);
    }

    /**
     * 10.2.1.2.8 HasThisBinding ()
     */
    @Override
    public boolean hasThisBinding() {
        return false;
    }

    /**
     * -
     */
    @Override
    public Object getThisBinding() {
        throw new IllegalStateException();
    }

    /**
     * 10.2.1.2.9 HasSuperBinding ()
     */
    @Override
    public boolean hasSuperBinding() {
        return false;
    }

    /**
     * 10.2.1.2.10 WithBaseObject()
     */
    @Override
    public ScriptObject withBaseObject() {
        if (withEnvironment) {
            return bindings;
        }
        return null;
    }

}
