/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * Integrity level of objects.
 * 
 * @see AbstractOperations#SetIntegrityLevel(ExecutionContext, ScriptObject, IntegrityLevel)
 * @see AbstractOperations#TestIntegrityLevel(ExecutionContext, ScriptObject, IntegrityLevel)
 */
public enum IntegrityLevel {
    /**
     * Integrity level for sealed objects.
     */
    Sealed,

    /**
     * Integrity level for frozen objects.
     */
    Frozen
}
