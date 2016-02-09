/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newReferenceError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>8 Executable Code and Execution Contexts</h1><br>
 * <h2>8.1 Lexical Environments</h2><br>
 * <h3>8.1.1 Environment Records</h3>
 * <ul>
 * <li>8.1.1.2 Object Environment Records
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
        return String.format("%s: {%n\twithEnvironment=%b,%n\tbindings=%n\t%s%n}", getClass()
                .getSimpleName(), withEnvironment, bindings);
    }

    @Override
    public Set<String> bindingNames() {
        HashSet<String> names = new HashSet<>();
        for (Iterator<?> keys = bindings.enumerateKeys(cx); keys.hasNext();) {
            Object key = keys.next();
            Object propertyKey = ToPropertyKey(cx, key);
            if (propertyKey instanceof String) {
                names.add((String) propertyKey);
            }
        }
        return names;
    }

    /**
     * 8.1.1.2.1 HasBinding(N)
     */
    @Override
    public boolean hasBinding(String name) {
        /* step 1 (omitted) */
        /* step 2 */
        ScriptObject bindings = this.bindings;
        /* steps 3-4 */
        boolean foundBinding = HasProperty(cx, bindings, name);
        /* step 5 */
        if (!foundBinding) {
            return false;
        }
        /* step 6 */
        if (!withEnvironment) {
            return true;
        }
        /* steps 7-8 */
        Object unscopables = Get(cx, bindings, BuiltinSymbol.unscopables.get());
        /* step 9 */
        if (Type.isObject(unscopables)) {
            boolean blocked = ToBoolean(Get(cx, Type.objectValue(unscopables), name));
            if (blocked) {
                return false;
            }
        }
        /* step 10 */
        return true;
    }

    /**
     * 8.1.1.2.2 CreateMutableBinding (N,D)
     */
    @Override
    public void createMutableBinding(String name, boolean deletable) {
        /* steps 1-2 (omitted) */
        /* steps 3-4 */
        PropertyDescriptor desc = new PropertyDescriptor(UNDEFINED, true, true, deletable);
        DefinePropertyOrThrow(cx, bindings, name, desc);
    }

    /**
     * 8.1.1.2.3 CreateImmutableBinding (N, S)
     */
    @Override
    public void createImmutableBinding(String name, boolean strict) {
        throw new IllegalStateException();
    }

    /**
     * 8.1.1.2.4 InitializeBinding (N,V)
     */
    @Override
    public void initializeBinding(String name, Object value) {
        assert value != null;
        /* step 1 (omitted) */
        /* steps 2-3 (not applicable) */
        /* step 4 */
        setMutableBinding(name, value, false);
    }

    /**
     * 8.1.1.2.5 SetMutableBinding (N,V,S)
     */
    @Override
    public void setMutableBinding(String name, Object value, boolean strict) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        Set(cx, bindings, name, value, strict);
    }

    /**
     * 8.1.1.2.6 GetBindingValue(N,S)
     */
    @Override
    public Object getBindingValue(String name, boolean strict) {
        /* step 1 (omitted) */
        /* step 2 */
        ScriptObject bindings = this.bindings;
        /* steps 3-4 */
        boolean foundBinding = HasProperty(cx, bindings, name);
        /* step 5 */
        if (!foundBinding) {
            if (!strict) {
                return UNDEFINED;
            }
            throw newReferenceError(cx, Messages.Key.UnresolvableReference, name);
        }
        /* step 6 */
        return Get(cx, bindings, name);
    }

    /**
     * 8.1.1.2.7 DeleteBinding (N)
     */
    @Override
    public boolean deleteBinding(String name) {
        /* steps 1-2 (omitted) */
        /* step 3 */
        return bindings.delete(cx, name);
    }

    /**
     * 8.1.1.2.8 HasThisBinding ()
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
    public Object getThisBinding(ExecutionContext cx) {
        throw new IllegalStateException();
    }

    /**
     * 8.1.1.2.9 HasSuperBinding ()
     */
    @Override
    public boolean hasSuperBinding() {
        /* step 1 */
        return false;
    }

    /**
     * 8.1.1.2.10 WithBaseObject()
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
