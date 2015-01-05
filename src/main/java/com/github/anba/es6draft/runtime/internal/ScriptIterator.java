/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.Iterator;

import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * {@link Iterator} providing access to an underlying {@link ScriptObject}.
 */
public interface ScriptIterator<E> extends Iterator<E> {
    /**
     * Returns the script iterator object.
     * 
     * @return the script iterator object
     */
    ScriptObject getScriptObject();
}
