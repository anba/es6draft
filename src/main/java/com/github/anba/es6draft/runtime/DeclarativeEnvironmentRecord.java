/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.internal.Errors.throwReferenceError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>10 Executable Code and Execution Contexts</h1><br>
 * <h2>10.2 Lexical Environments</h2><br>
 * <h3>10.2.1 Environment Records</h3>
 * <ul>
 * <li>10.2.1.1 Declarative Environment Records
 * </ul>
 */
public class DeclarativeEnvironmentRecord implements EnvironmentRecord {
    private static class Binding {
        final boolean mutable;
        final boolean deletable;
        Object value;

        public Binding(boolean mutable, boolean deletable) {
            this.mutable = mutable;
            this.deletable = deletable;
        }

        @Override
        public String toString() {
            return String.format("{value = %s, mutable = %b, deletable = %b}", value, mutable,
                    deletable);
        }
    }

    protected final ExecutionContext cx;
    private Map<String, Binding> bindings = new HashMap<>();

    public DeclarativeEnvironmentRecord(ExecutionContext cx) {
        this.cx = cx;
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
        for (Iterator<Entry<KEY, VALUE>> iter = map.entrySet().iterator();;) {
            Entry<KEY, VALUE> entry = iter.next();
            sb.append("\n  ").append(entry.getKey()).append('=').append(entry.getValue());
            if (!iter.hasNext())
                break;
            sb.append(',');
        }
        return sb.append('\n').append('}').toString();
    }

    /**
     * 10.2.1.1.1 HasBinding(N)
     */
    @Override
    public boolean hasBinding(String name) {
        return bindings.containsKey(name);
    }

    /**
     * 10.2.1.1.2 CreateMutableBinding (N,D)
     */
    @Override
    public void createMutableBinding(String name, boolean deletable) {
        assert !bindings.containsKey(name);
        bindings.put(name, new Binding(true, deletable));
    }

    /**
     * 10.2.1.1.3 CreateImmutableBinding (N)
     */
    @Override
    public void createImmutableBinding(String name) {
        assert !bindings.containsKey(name);
        bindings.put(name, new Binding(false, false));
    }

    /**
     * 10.2.1.1.4 InitialiseBinding (N,V)
     */
    @Override
    public void initialiseBinding(String name, Object value) {
        assert value != null;
        Binding b = bindings.get(name);
        assert b != null && b.value == null;
        b.value = value;
    }

    /**
     * 10.2.1.1.5 SetMutableBinding (N,V,S)
     */
    @Override
    public void setMutableBinding(String name, Object value, boolean strict) {
        assert value != null;
        Binding b = bindings.get(name);
        assert b != null; // FIXME: spec bug (bug 159)
        if (b.value == null) {
            throw throwReferenceError(cx, Messages.Key.UninitialisedBinding, name);
        } else if (b.mutable) {
            b.value = value;
        } else if (strict) {
            throw throwTypeError(cx, Messages.Key.ImmutableBinding, name);
        }
    }

    /**
     * 10.2.1.1.6 GetBindingValue(N,S)
     */
    @Override
    public Object getBindingValue(String name, boolean strict) {
        Binding b = bindings.get(name);
        assert b != null;
        if (b.value == null) {
            if (!strict) {
                return UNDEFINED;
            }
            throw throwReferenceError(cx, Messages.Key.UninitialisedBinding, name);
        }
        return b.value;
    }

    /**
     * 10.2.1.1.7 DeleteBinding (N)
     */
    @Override
    public boolean deleteBinding(String name) {
        Binding b = bindings.get(name);
        if (b == null) {
            return true;
        }
        if (!b.deletable) {
            return false;
        }
        bindings.remove(name);
        return true;
    }

    /**
     * 10.2.1.1.8 HasThisBinding ()
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
     * 10.2.1.1.9 HasSuperBinding ()
     */
    @Override
    public boolean hasSuperBinding() {
        return false;
    }

    /**
     * 10.2.1.1.10 WithBaseObject()
     */
    @Override
    public ScriptObject withBaseObject() {
        return null;
    }
}
