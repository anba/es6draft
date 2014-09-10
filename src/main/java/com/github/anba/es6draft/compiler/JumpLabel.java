/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import org.objectweb.asm.Label;

/**
 * Enhanced {@link Label} to track usage
 */
abstract class JumpLabel extends Label {
    protected boolean used = false;

    /**
     * Label for {@code break} statements
     */
    static final class BreakLabel extends JumpLabel {
    }

    /**
     * Label for {@code continue} statements
     */
    static final class ContinueLabel extends JumpLabel {
    }

    /**
     * Label for {@code return} statements
     */
    static final class ReturnLabel extends JumpLabel {
    }

    /**
     * Wraps an existing {@link JumpLabel}
     */
    static final class TempLabel extends JumpLabel {
        private final JumpLabel actual;

        TempLabel(JumpLabel actual) {
            this.actual = actual;
        }

        /**
         * Returns the wrapped {@link JumpLabel}.
         * 
         * @return the wrapped label
         */
        JumpLabel getActual() {
            return actual;
        }
    }

    /**
     * Mark this label as being used.
     * 
     * @return this label object
     */
    final JumpLabel mark() {
        used = true;
        return this;
    }

    /**
     * Returns {@code true} if this label is used, otherwise {@code false}.
     * 
     * @return {@code true} if this label is used
     */
    final boolean isUsed() {
        return used;
    }
}
