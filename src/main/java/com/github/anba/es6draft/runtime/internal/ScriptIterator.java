/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.Iterator;
import java.util.function.Consumer;

import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * {@link Iterator} providing access to an underlying {@link ScriptObject}.
 */
public interface ScriptIterator<E> extends Iterator<E> {
    /**
     * Gets or creates the underlying script object iterator.
     * 
     * @return the script object iterator
     */
    ScriptObject getScriptObject();

    Object nextIterResult() throws ScriptException;

    Object nextIterResult(Object value) throws ScriptException;

    /**
     * {@inheritDoc}
     * 
     * @throws ScriptException
     *             if an execution error occurs in the scripted iterator
     */
    @Override
    boolean hasNext() throws ScriptException;

    /**
     * {@inheritDoc}
     * 
     * @throws ScriptException
     *             if an execution error occurs in the scripted iterator
     */
    @Override
    E next() throws ScriptException;

    /**
     * Closes the iterator.
     * 
     * @throws ScriptException
     *             if an execution error occurs in the scripted iterator
     */
    void close() throws ScriptException;

    /**
     * Closes the iterator.
     * 
     * @param cause
     *            the exception cause
     * @throws ScriptException
     *             if an execution error occurs in the scripted iterator
     */
    void close(Throwable cause) throws ScriptException;

    /**
     * <strong>The consumer <u>must not</u> invoke script code!</strong>
     * <p>
     * {@inheritDoc}
     */
    @Override
    default void forEachRemaining(Consumer<? super E> action) {
        Iterator.super.forEachRemaining(action);
    }
}
