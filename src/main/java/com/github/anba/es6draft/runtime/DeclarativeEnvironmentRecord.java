/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.internal.Errors.newReferenceError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
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
    public static abstract class Binding implements Cloneable {
        final boolean mutable;
        final boolean deletable;
        final boolean strict;

        Binding(boolean mutable, boolean deletable, boolean strict) {
            this.mutable = mutable;
            this.deletable = deletable;
            this.strict = strict;
        }

        public final boolean isMutable() {
            return mutable;
        }

        public final boolean isDeletable() {
            return deletable;
        }

        public final boolean isStrict() {
            return strict;
        }

        public abstract boolean isInitialized();

        public abstract void initialize(Object value);

        @Override
        public abstract Binding clone();

        public abstract void setValue(Object value);

        public abstract Object getValue();
    }

    private static final class DirectBinding extends Binding {
        private Object value;

        DirectBinding(boolean mutable, boolean deletable, boolean strict) {
            super(mutable, deletable, strict);
        }

        @Override
        public DirectBinding clone() {
            DirectBinding clone = new DirectBinding(mutable, deletable, strict);
            clone.value = value;
            return clone;
        }

        @Override
        public String toString() {
            return String.format("{mutable = %b, deletable = %b, strict = %b, value = %s}",
                    mutable, deletable, strict, Objects.toString(value, "<uninitialized>"));
        }

        @Override
        public boolean isInitialized() {
            return value != null;
        }

        @Override
        public void initialize(Object value) {
            assert this.value == null : "binding already initialized";
            this.value = value;
        }

        @Override
        public void setValue(Object newValue) {
            assert newValue != null && this.value != null && mutable;
            this.value = newValue;
        }

        @Override
        public Object getValue() {
            assert value != null;
            return value;
        }
    }

    private final ExecutionContext cx;
    private final boolean catchEnvironment;
    private final HashMap<String, Binding> bindings;

    public DeclarativeEnvironmentRecord(ExecutionContext cx, boolean catchEnvironment) {
        this.cx = cx;
        this.catchEnvironment = catchEnvironment;
        this.bindings = new HashMap<>();
    }

    DeclarativeEnvironmentRecord(DeclarativeEnvironmentRecord source) {
        this.cx = source.cx;
        this.catchEnvironment = source.catchEnvironment;
        this.bindings = source.cloneBindings();
    }

    private HashMap<String, Binding> cloneBindings() {
        HashMap<String, Binding> newBindings = new HashMap<>();
        for (Map.Entry<String, Binding> entry : bindings.entrySet()) {
            String name = entry.getKey();
            Binding binding = entry.getValue();
            assert binding.isInitialized() : "binding not initialized: " + name;
            newBindings.put(name, binding.clone());
        }
        return newBindings;
    }

    protected final void createBinding(String name, Binding binding) {
        assert name != null && binding != null;
        assert !bindings.containsKey(name) : "binding redeclaration: " + name;
        bindings.put(name, binding);
    }

    public final Binding getBinding(String name) {
        assert name != null : "null name for binding";
        Binding b = bindings.get(name);
        assert b != null : "binding not found: " + name;
        return b;
    }

    @Override
    public String toString() {
        return String.format("%s: {%n\tbindings=%s%n}", getClass().getSimpleName(),
                bindingsToString());
    }

    /*package*/String bindingsToString() {
        return toString(bindings);
    }

    private static <KEY, VALUE> String toString(Map<KEY, VALUE> map) {
        if (map.isEmpty()) {
            return "{}";
        }
        try (Formatter f = new Formatter(new StringBuilder(), null)) {
            f.format("{");
            for (Iterator<Map.Entry<KEY, VALUE>> iter = map.entrySet().iterator();;) {
                Map.Entry<KEY, VALUE> entry = iter.next();
                f.format("%n\t\t%s=%s", entry.getKey(), entry.getValue());
                if (!iter.hasNext())
                    break;
                f.format(",");
            }
            f.format("%n\t}");
            return f.toString();
        }
    }

    @Override
    public final Set<String> bindingNames() {
        return Collections.unmodifiableSet(bindings.keySet());
    }

    @Override
    public final Object getBindingValueOrNull(String name, boolean strict) {
        Binding b = bindings.get(name);
        if (b == null) {
            return null;
        }
        if (!b.isInitialized()) {
            throw newReferenceError(cx, Messages.Key.UninitializedBinding, name);
        }
        return b.getValue();
    }

    @Override
    public final Reference<DeclarativeEnvironmentRecord, String> getReferenceOrNull(String name,
            boolean strict) {
        Binding b = bindings.get(name);
        if (b == null) {
            return null;
        }
        if (b.deletable) {
            return new Reference.IdentifierReference<>(this, name, strict);
        }
        return new Reference.BindingReference(this, b, name, strict);
    }

    /**
     * 8.1.1.1.1 HasBinding(N)
     */
    @Override
    public final boolean hasBinding(String name) {
        /* step 1 (omitted) */
        /* steps 2-3 */
        return bindings.containsKey(name);
    }

    /**
     * 8.1.1.1.2 CreateMutableBinding (N,D)
     */
    @Override
    public final void createMutableBinding(String name, boolean deletable) {
        /* step 1 (omitted) */
        /* step 2 */
        assert !bindings.containsKey(name) : "binding redeclaration: " + name;
        /* steps 3-4 */
        bindings.put(name, new DirectBinding(true, deletable, false));
    }

    /**
     * 8.1.1.1.3 CreateImmutableBinding (N, S)
     */
    @Override
    public final void createImmutableBinding(String name, boolean strict) {
        /* step 1 (omitted) */
        /* step 2 */
        assert !bindings.containsKey(name) : "binding redeclaration: " + name;
        /* step 3 */
        bindings.put(name, new DirectBinding(false, false, strict));
        /* step 4 (return) */
    }

    /**
     * 8.1.1.1.4 InitializeBinding (N,V)
     */
    @Override
    public final void initializeBinding(String name, Object value) {
        assert value != null;
        Binding b = bindings.get(name);
        /* step 1 (omitted) */
        /* step 2 */
        assert b != null : "binding not found: " + name;
        assert !b.isInitialized() : "binding already initialized: " + name;
        /* steps 3-4 */
        b.initialize(value);
        /* step 5 (return) */
    }

    /**
     * 8.1.1.1.5 SetMutableBinding (N,V,S)
     */
    @Override
    public final void setMutableBinding(String name, Object value, boolean strict) {
        assert value != null;
        Binding b = bindings.get(name);
        /* step 1 (omitted) */
        /* step 2 */
        if (b == null) {
            if (strict) {
                throw newReferenceError(cx, Messages.Key.UnresolvableReference, name);
            }
            createMutableBinding(name, true);
            initializeBinding(name, value);
            return;
        }
        /* steps 3-7 */
        if (!b.isInitialized()) {
            throw newReferenceError(cx, Messages.Key.UninitializedBinding, name);
        } else if (b.mutable) {
            b.setValue(value);
        } else if (strict || b.isStrict()) {
            throw newTypeError(cx, Messages.Key.ImmutableBinding, name);
        }
    }

    /**
     * 8.1.1.1.6 GetBindingValue(N,S)
     */
    @Override
    public final Object getBindingValue(String name, boolean strict) {
        Binding b = bindings.get(name);
        /* step 1 (omitted) */
        /* step 2 */
        assert b != null : "binding not found: " + name;
        /* step 3 */
        if (!b.isInitialized()) {
            throw newReferenceError(cx, Messages.Key.UninitializedBinding, name);
        }
        /* step 4 */
        return b.getValue();
    }

    /**
     * 8.1.1.1.7 DeleteBinding (N)
     */
    @Override
    public final boolean deleteBinding(String name) {
        Binding b = bindings.get(name);
        /* step 1 (omitted) */
        /* step 2 */
        assert b != null;
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
    public Object getThisBinding(ExecutionContext cx) {
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
    public final ScriptObject withBaseObject() {
        /* step 1 */
        return null;
    }

    /**
     * Returns {@code true} if this declarative environment was created for a catch clause.
     * 
     * @return {@code true} if this environment record is the environment of a catch clause
     */
    public final boolean isCatchEnvironment() {
        return catchEnvironment;
    }
}
