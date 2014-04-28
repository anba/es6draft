/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.internal.Errors.newReferenceError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>8 Executable Code and Execution Contexts</h1><br>
 * <h2>8.1 Lexical Environments</h2><br>
 * <h3>8.1.1 Environment Records</h3>
 * <ul>
 * <li>8.1.1.1 Declarative Environment Records
 * </ul>
 */
public class DeclarativeEnvironmentRecord implements EnvironmentRecord {
    public static final class Binding implements Cloneable {
        final boolean mutable;
        final boolean deletable;
        Object value;

        Binding(boolean mutable, boolean deletable) {
            this.mutable = mutable;
            this.deletable = deletable;
        }

        @Override
        public Binding clone() {
            Binding clone = new Binding(mutable, deletable);
            clone.value = value;
            return clone;
        }

        @Override
        public String toString() {
            return String.format("{value = %s, mutable = %b, deletable = %b}", value, mutable,
                    deletable);
        }

        public void initialize(Object value) {
            assert this.value == null && value != null;
            this.value = value;
        }

        public void setValue(Object value) {
            assert this.value != null && value != null && this.mutable;
            this.value = value;
        }

        public Object getValue() {
            assert this.value != null;
            return value;
        }

        public Reference<Binding, String> toReference(String name, boolean strict) {
            return new Reference.BindingReference(this, name, strict);
        }

        public void setValue(ExecutionContext cx, String name, Object value, boolean strict) {
            assert value != null;
            if (this.value == null) {
                throw newReferenceError(cx, Messages.Key.UninitializedBinding, name);
            } else if (mutable) {
                this.value = value;
            } else if (strict) {
                throw newTypeError(cx, Messages.Key.ImmutableBinding, name);
            }
        }

        public Object getValue(ExecutionContext cx, String name, boolean strict) {
            if (value == null) {
                if (!strict) {
                    return UNDEFINED;
                }
                throw newReferenceError(cx, Messages.Key.UninitializedBinding, name);
            }
            return value;
        }
    }

    protected final ExecutionContext cx;
    private final HashMap<String, Binding> bindings = new HashMap<>();

    public DeclarativeEnvironmentRecord(ExecutionContext cx) {
        this.cx = cx;
    }

    public final Binding getBinding(String name) {
        assert name != null : "null name for binding";
        Binding b = bindings.get(name);
        assert b != null : "binding not found: " + name;
        return b;
    }

    @Override
    public String toString() {
        return String.format("%s: {bindings=%s}", getClass().getSimpleName(), toString(bindings));
    }

    private static <KEY, VALUE> String toString(Map<KEY, VALUE> map) {
        if (map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (Iterator<Map.Entry<KEY, VALUE>> iter = map.entrySet().iterator();;) {
            Map.Entry<KEY, VALUE> entry = iter.next();
            sb.append("\n  ").append(entry.getKey()).append('=').append(entry.getValue());
            if (!iter.hasNext())
                break;
            sb.append(',');
        }
        return sb.append('\n').append('}').toString();
    }

    /**
     * Copies this record's bindings to {@code target}.
     * 
     * @param target
     *            the target record
     */
    void copyBindings(DeclarativeEnvironmentRecord target) {
        assert target.bindings.isEmpty() : "target bindings not empty";
        for (Map.Entry<String, Binding> entry : bindings.entrySet()) {
            String name = entry.getKey();
            Binding binding = entry.getValue();
            assert binding.value != null : "binding already initialized: " + name;
            target.bindings.put(name, binding.clone());
        }
    }

    /**
     * Tests if an existing binding is already initialized.
     * 
     * @param name
     *            the binding name
     * @return {@code true} if the binding is initialized
     */
    public boolean isInitialized(String name) {
        Binding b = bindings.get(name);
        assert b != null : "binding not found: " + name;
        return b.value != null;
    }

    @Override
    public Set<String> bindingNames() {
        return Collections.unmodifiableSet(bindings.keySet());
    }

    /**
     * 8.1.1.1.1 HasBinding(N)
     */
    @Override
    public boolean hasBinding(String name) {
        /* step 1 (omitted) */
        /* steps 2-3 */
        return bindings.containsKey(name);
    }

    /**
     * 8.1.1.1.2 CreateMutableBinding (N,D)
     */
    @Override
    public void createMutableBinding(String name, boolean deletable) {
        /* step 1 (omitted) */
        /* step 2 */
        assert !bindings.containsKey(name) : "binding redeclaration: " + name;
        /* steps 3-4 */
        bindings.put(name, new Binding(true, deletable));
    }

    /**
     * 8.1.1.1.3 CreateImmutableBinding (N)
     */
    @Override
    public void createImmutableBinding(String name) {
        /* step 1 (omitted) */
        /* step 2 */
        assert !bindings.containsKey(name) : "binding redeclaration: " + name;
        /* step 3 */
        bindings.put(name, new Binding(false, false));
    }

    /**
     * 8.1.1.1.4 InitializeBinding (N,V)
     */
    @Override
    public void initializeBinding(String name, Object value) {
        assert value != null;
        Binding b = bindings.get(name);
        /* step 1 (omitted) */
        /* step 2 */
        assert b != null : "binding not found: " + name;
        assert b.value == null : "binding already initialized: " + name;
        /* steps 3-4 */
        b.value = value;
    }

    /**
     * 8.1.1.1.5 SetMutableBinding (N,V,S)
     */
    @Override
    public void setMutableBinding(String name, Object value, boolean strict) {
        assert value != null;
        Binding b = bindings.get(name);
        /* step 1 (omitted) */
        /* step 2 */
        assert b != null : "binding not found: " + name; // FIXME: spec bug (bug 159)
        /* steps 3-6 */
        if (b.value == null) {
            throw newReferenceError(cx, Messages.Key.UninitializedBinding, name);
        } else if (b.mutable) {
            b.value = value;
        } else if (strict) {
            throw newTypeError(cx, Messages.Key.ImmutableBinding, name);
        }
    }

    /**
     * 8.1.1.1.6 GetBindingValue(N,S)
     */
    @Override
    public Object getBindingValue(String name, boolean strict) {
        Binding b = bindings.get(name);
        /* step 1 (omitted) */
        /* step 2 */
        assert b != null : "binding not found: " + name;
        /* step 3 */
        if (b.value == null) {
            throw newReferenceError(cx, Messages.Key.UninitializedBinding, name);
        }
        /* step 4 */
        return b.value;
    }

    /**
     * 8.1.1.1.7 DeleteBinding (N)
     */
    @Override
    public boolean deleteBinding(String name) {
        Binding b = bindings.get(name);
        /* step 1 (omitted) */
        /* step 2 */
        if (b == null) {
            return true;
        }
        /* step 3 */
        if (!b.deletable) {
            return false;
        }
        /* step 4 */
        bindings.remove(name);
        /* step 5 */
        return true;
    }

    /**
     * 8.1.1.1.8 HasThisBinding ()
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
     * 8.1.1.1.9 HasSuperBinding ()
     */
    @Override
    public boolean hasSuperBinding() {
        /* step 1 */
        return false;
    }

    /**
     * 8.1.1.1.10 WithBaseObject()
     */
    @Override
    public ScriptObject withBaseObject() {
        /* step 1 */
        return null;
    }
}
