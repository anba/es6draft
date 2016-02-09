/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.compiler.assembler.Jump;

/**
 * Specialized {@link Jump} objects.
 */
final class Labels {
    private Labels() {
    }

    /**
     * Label for {@code break} statements.
     */
    static final class BreakLabel extends Jump {
    }

    /**
     * Label for {@code continue} statements.
     */
    static final class ContinueLabel extends Jump {
    }

    /**
     * Label for {@code return} statements.
     */
    static final class ReturnLabel extends Jump {
    }

    /**
     * Wraps an existing {@link Jump}.
     */
    static final class TempLabel extends Jump {
        private final Jump wrapped;

        TempLabel(Jump wrapped) {
            this.wrapped = wrapped;
        }

        /**
         * Returns the wrapped {@link Jump}.
         * 
         * @return the wrapped label
         */
        Jump getWrapped() {
            return wrapped;
        }

        /**
         * Recursively unwraps this temp label.
         * 
         * @return the wrapped label
         */
        Jump unwrap() {
            Jump w = wrapped;
            while (w instanceof TempLabel) {
                w = ((TempLabel) w).wrapped;
            }
            return w;
        }

        /**
         * Returns {@code true} if the wrapped label is a {@code return} label.
         * 
         * @return {@code true} if {@code return} label
         */
        boolean isReturn() {
            return unwrap() instanceof ReturnLabel;
        }

        @Override
        public String toString() {
            return String.format("TempLabel [%s]", unwrap().getClass().getSimpleName());
        }
    }
}
