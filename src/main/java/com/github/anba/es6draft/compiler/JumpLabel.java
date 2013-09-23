/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import org.objectweb.asm.Label;

/**
 * Enhanced {@link Label} to track usage
 */
class JumpLabel extends Label {
    private boolean used = false;

    /**
     * Mark this label as being used
     */
    JumpLabel mark() {
        used = true;
        return this;
    }

    /**
     * Returns {@code true} if this label is used, otherwise {@code false}
     */
    boolean isUsed() {
        return used;
    }
}
