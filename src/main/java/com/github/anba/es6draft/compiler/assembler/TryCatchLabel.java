/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

/**
 * Label class for try-catch handlers.
 */
public final class TryCatchLabel {
    private final org.objectweb.asm.Label label;

    public TryCatchLabel() {
        this.label = new org.objectweb.asm.Label();
    }

    /*package*/org.objectweb.asm.Label label() {
        return label;
    }
}
