/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

/**
 * 
 */
public class Test262AssertionError extends AssertionError {
    private static final long serialVersionUID = -727900497296323773L;

    public Test262AssertionError(Object detailMessage) {
        super(detailMessage);
    }
}
