/**
 * Copyright (c) 2012-2014 André Bargull
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
        return String.format("%s: {bindings=%s}", getClass().getSimpleName(), bindings);
    }

    @Override
    public Set<String> bindingNames() {
        HashSet<String> names = new HashSet<>();
        Iterator<?> keys = bindings.enumerateKeys(cx);
        while (keys.hasNext()) {
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
        // FIXME: spec bug - only consult @@unscopables if withEnvironment is true?
        if (!withEnvironment) {
            return HasProperty(cx, bindings, name);
        }
        /* step 1 (omitted) */
        /* step 2 */
        ScriptObject bindings = this.bindings;
        /* step 3 */
        while (bindings != null) {
            boolean hasOwn = HasOwnProperty(cx, bindings, name);
            if (hasOwn) {
                boolean hasUnscopables = HasOwnProperty(cx, bindings,
                        BuiltinSymbol.unscopables.get());
                if (!hasUnscopables) {
                    return true;
                }
                Object unscopables = Get(cx, bindings, BuiltinSymbol.unscopables.get());
                if (!Type.isObject(unscopables)) {
                    return true;
                }
                boolean isBlocked = HasOwnProperty(cx, Type.objectValue(unscopables), name);
                if (!isBlocked) {
                    return true;
                }
            }
            bindings = bindings.getPrototypeOf(cx);
        }
        /* step 4 */
        return false;
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
     * 8.1.1.2.3 CreateImmutableBinding (N)
     */
    @Override
    public void createImmutableBinding(String name) {
        throw new IllegalStateException();
    }

    /**
     * 8.1.1.2.4 InitializeBinding (N,V)
     */
    @Override
    public void initializeBinding(String name, Object value) {
        assert value != null;
        /* step 1 (omitted) */
        /* step 2 */
        // TODO: uninitialized binding in an object environment record?!
        /* step 3 */
        // TODO: record the binding has been initialized
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
        Put(cx, bindings, name, value, strict);
    }

    /**
     * 8.1.1.2.6 GetBindingValue(N,S)
     */
    @Override
    public Object getBindingValue(String name, boolean strict) {
        // FIXME: spec bug - only consult @@unscopables if withEnvironment is true?
        if (!withEnvironment) {
            boolean value = HasProperty(cx, bindings, name);
            if (!value) {
                if (!strict) {
                    return UNDEFINED;
                }
                throw newReferenceError(cx, Messages.Key.UnresolvableReference, name);
            }
            return Get(cx, bindings, name);
        }
        /* step 1 (omitted) */
        /* step 2 */
        ScriptObject bindingsThis = this.bindings;
        /* step 3 */
        ScriptObject bindings = bindingsThis;
        /* step 4 */
        while (bindings != null) {
            boolean hasOwn = HasOwnProperty(cx, bindings, name);
            if (hasOwn) {
                boolean hasUnscopables = HasOwnProperty(cx, bindings,
                        BuiltinSymbol.unscopables.get());
                boolean isBlocked = false;
                if (hasUnscopables) {
                    Object unscopables = Get(cx, bindings, BuiltinSymbol.unscopables.get());
                    if (Type.isObject(unscopables)) {
                        isBlocked = HasOwnProperty(cx, Type.objectValue(unscopables), name);
                    }
                }
                if (!isBlocked) {
                    return bindings.get(cx, name, bindingsThis);
                }
            }
            bindings = bindings.getPrototypeOf(cx);
        }
        /* step 5 */
        if (!strict) {
            return UNDEFINED;
        }
        throw newReferenceError(cx, Messages.Key.UnresolvableReference, name);
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
    public Object getThisBinding() {
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
