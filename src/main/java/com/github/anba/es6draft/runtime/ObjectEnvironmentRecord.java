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
    private final ExecutionContext cx;
    private final ScriptObject bindings;
    private final boolean withEnvironment;

    public ObjectEnvironmentRecord(ExecutionContext cx, ScriptObject bindings,
            boolean withEnvironment) {
        this.cx = cx;
        this.bindings = bindings;
        this.withEnvironment = withEnvironment;
    }

    @Override
    public String toString() {
        return String.format("%s: {bindings=%s}", getClass().getSimpleName(), bindings);
    }

    /**
     * 10.2.1.2.1 HasBinding(N)
     */
    @Override
    public boolean hasBinding(String name) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        return HasProperty(cx, bindings, name);
    }

    /**
     * 10.2.1.2.2 CreateMutableBinding (N,D)
     */
    @Override
    public void createMutableBinding(String name, boolean deletable) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        assert HasProperty(cx, bindings, name) == false; // FIXME: spec bug (bug 1786)
        /* steps 4-5 */
        PropertyDescriptor desc = new PropertyDescriptor(UNDEFINED, true, true, deletable);
        DefinePropertyOrThrow(cx, bindings, name, desc);
    }

    /**
     * 10.2.1.2.3 CreateImmutableBinding (N)
     */
    @Override
    public void createImmutableBinding(String name) {
        throw new IllegalStateException();
    }

    /**
     * 10.2.1.2.4 InitialiseBinding (N,V)
     */
    @Override
    public void initialiseBinding(String name, Object value) {
        assert value != null;
        /* step 1 (omitted) */
        /* step 2 */
        // TODO: uninitialised binding in an object environment record?!
        /* step 3 */
        // TODO: record the binding has been initialised
        /* step 4 */
        setMutableBinding(name, value, false);
    }

    /**
     * 10.2.1.2.5 SetMutableBinding (N,V,S)
     */
    @Override
    public void setMutableBinding(String name, Object value, boolean strict) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        Put(cx, bindings, name, value, strict);
    }

    /**
     * 10.2.1.2.6 GetBindingValue(N,S)
     */
    @Override
    public Object getBindingValue(String name, boolean strict) {
        /* steps 1-2 (omitted) */
        /* steps 3-4 */
        boolean value = HasProperty(cx, bindings, name);
        /* step 5 */
        if (!value) {
            if (!strict) {
                return UNDEFINED;
            }
            throw throwReferenceError(cx, Messages.Key.UnresolvableReference, name);
        }
        /* step 6 */
        return Get(cx, bindings, name);
    }

    /**
     * 10.2.1.2.7 DeleteBinding (N)
     */
    @Override
    public boolean deleteBinding(String name) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        return bindings.delete(cx, name);
    }

    /**
     * 10.2.1.2.8 HasThisBinding ()
     */
    @Override
    public boolean hasThisBinding() {
        /* step 1 */
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
        /* step 1 */
        return false;
    }

    /**
     * 10.2.1.2.10 WithBaseObject()
     */
    @Override
    public ScriptObject withBaseObject() {
        /* step 1 (omitted) */
        /* step 2 */
        if (withEnvironment) {
            return bindings;
        }
        /* step 3 */
        return null;
    }
}
