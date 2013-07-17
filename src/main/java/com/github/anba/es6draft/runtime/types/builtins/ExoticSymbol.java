/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.CreateEmptyIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.4 Symbol Exotic Objects
 * </ul>
 */
public final class ExoticSymbol implements ScriptObject {
    private final String name;

    public ExoticSymbol(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /** 8.4.4.1 [[GetInheritance]] ( ) */
    @Override
    public ScriptObject getInheritance(ExecutionContext cx) {
        return null;
    }

    /** 8.4.4.2 [[SetInheritance]] (V) */
    @Override
    public boolean setInheritance(ExecutionContext cx, ScriptObject prototype) {
        return false;
    }

    /** 8.4.4.3 [[IsExtensible]] ( ) */
    @Override
    public boolean isExtensible(ExecutionContext cx) {
        return false;
    }

    /** 8.4.4.4 [[PreventExtensions]] ( ) */
    @Override
    public boolean preventExtensions(ExecutionContext cx) {
        return true;
    }

    /** 8.4.4.5 [[HasOwnProperty]] (P) 8.4.4.5 */
    @Override
    public boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        return false;
    }

    /** 8.4.4.5 [[HasOwnProperty]] (P) */
    @Override
    public boolean hasOwnProperty(ExecutionContext cx, ExoticSymbol propertyKey) {
        return false;
    }

    /** 8.4.4.6 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        return null;
    }

    /** 8.4.4.6 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, ExoticSymbol propertyKey) {
        return null;
    }

    /** 8.4.4.7 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        return false;
    }

    /** 8.4.4.7 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, ExoticSymbol propertyKey,
            PropertyDescriptor desc) {
        return false;
    }

    /** 8.4.4.8 [[HasProperty]] (P) */
    @Override
    public boolean hasProperty(ExecutionContext cx, String propertyKey) {
        return false;
    }

    /** 8.4.4.8 [[HasProperty]] (P) */
    @Override
    public boolean hasProperty(ExecutionContext cx, ExoticSymbol propertyKey) {
        return false;
    }

    /** 8.4.4.9 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        return UNDEFINED;
    }

    /** 8.4.4.9 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, ExoticSymbol propertyKey, Object receiver) {
        return UNDEFINED;
    }

    /** 8.4.4.10 [[Set]] ( P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        return false;
    }

    /** 8.4.4.10 [[Set]] ( P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, ExoticSymbol propertyKey, Object value, Object receiver) {
        return false;
    }

    /** 8.4.4.11 [[Invoke]] (P, ArgumentsList, Receiver) */
    @Override
    public Object invoke(ExecutionContext cx, String propertyKey, Object[] arguments,
            Object receiver) {
        throw throwTypeError(cx, Messages.Key.NotCallable);
    }

    /** 8.4.4.11 [[Invoke]] (P, ArgumentsList, Receiver) */
    @Override
    public Object invoke(ExecutionContext cx, ExoticSymbol propertyKey, Object[] arguments,
            Object receiver) {
        throw throwTypeError(cx, Messages.Key.NotCallable);
    }

    /** 8.4.4.12 [[Delete]] (P) */
    @Override
    public boolean delete(ExecutionContext cx, String propertyKey) {
        return true;
    }

    /** 8.4.4.12 [[Delete]] (P) */
    @Override
    public boolean delete(ExecutionContext cx, ExoticSymbol propertyKey) {
        return true;
    }

    /** 8.4.4.13 [[Enumerate]] () */
    @Override
    public ScriptObject enumerate(ExecutionContext cx) {
        return CreateEmptyIterator(cx);
    }

    /** 8.4.4.14 [[OwnPropertyKeys]] ( ) */
    @Override
    public ScriptObject ownPropertyKeys(ExecutionContext cx) {
        return CreateEmptyIterator(cx);
    }
}
