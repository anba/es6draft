/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * Interface for objects with post-construction initialization.
 */
public interface Initializable {

    /**
     * The initialization method for this object.
     * 
     * @param cx
     *            the execution context
     */
    void initialize(ExecutionContext cx);
}
