/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static com.github.anba.es6draft.runtime.internal.Errors.newReferenceError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.scripting.TypeConverter.fromJava;
import static com.github.anba.es6draft.scripting.TypeConverter.toJava;

import java.util.Set;

import javax.script.ScriptContext;

import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Reference;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * 
 */
final class ScriptContextEnvironmentRecord implements EnvironmentRecord {
    private final ExecutionContext cx;
    private final ScriptContext context;

    public ScriptContextEnvironmentRecord(ExecutionContext cx, ScriptContext context) {
        this.cx = cx;
        this.context = context;
    }

    @Override
    public Set<String> bindingNames() {
        return context.getBindings(ScriptContext.ENGINE_SCOPE).keySet();
    }

    @Override
    public Object getBindingValueOrNull(String name, boolean strict) {
        if (hasBinding(name)) {
            return getBindingValue(name, strict);
        }
        return null;
    }

    @Override
    public Reference<ScriptContextEnvironmentRecord, String> getReferenceOrNull(String name, boolean strict) {
        if (hasBinding(name)) {
            return new Reference.IdentifierReference<>(this, name, strict);
        }
        return null;
    }

    @Override
    public boolean hasBinding(String name) {
        return context.getAttributesScope(name) != -1;
    }

    @Override
    public void createMutableBinding(String name, boolean deletable) {
        setMutableBinding(name, UNDEFINED, false);
    }

    @Override
    public void createImmutableBinding(String name, boolean strict) {
        setMutableBinding(name, UNDEFINED, false);
    }

    @Override
    public void initializeBinding(String name, Object value) {
        setMutableBinding(name, value, false);
    }

    @Override
    public void setMutableBinding(String name, Object value, boolean strict) {
        context.setAttribute(name, toJava(value), ScriptContext.ENGINE_SCOPE);
    }

    @Override
    public Object getBindingValue(String name, boolean strict) {
        int scope = context.getAttributesScope(name);
        if (scope == -1) {
            if (strict) {
                throw newReferenceError(cx, Messages.Key.UnresolvableReference);
            }
            return UNDEFINED;
        }
        return fromJava(context.getAttribute(name, scope));
    }

    @Override
    public boolean deleteBinding(String name) {
        return context.removeAttribute(name, ScriptContext.ENGINE_SCOPE) != null;
    }

    @Override
    public boolean hasThisBinding() {
        return false;
    }

    @Override
    public boolean hasSuperBinding() {
        return false;
    }

    @Override
    public ScriptObject withBaseObject() {
        return null;
    }

    @Override
    public Object getThisBinding(ExecutionContext cx) {
        throw new IllegalStateException();
    }
}
