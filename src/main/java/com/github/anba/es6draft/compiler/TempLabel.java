/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

/**
 * Wraps an existing {@link JumpLabel}
 */
class TempLabel extends JumpLabel {
    private final JumpLabel actual;

    TempLabel(JumpLabel actual) {
        this.actual = actual;
    }

    /**
     * Returns the wrapped {@link JumpLabel}
     */
    JumpLabel getActual() {
        return actual;
    }

    @Override
    JumpLabel mark() {
        return this;
    }

    /**
     * Always throws an {@link IllegalStateException}, {@link TempLabel}s have no use count
     */
    @Override
    boolean isUsed() {
        throw new IllegalStateException();
    }
}
