/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 *
 */
public interface CreateAction<OBJECT extends ScriptObject> {
    /**
     * Creates a new object.
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor object
     * @param args
     *            the arguments array
     * @return the newly created object
     */
    OBJECT create(ExecutionContext cx, Constructor constructor, Object... args);
}
