/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import org.objectweb.asm.Label;

/**
 * Label class for try-catch handlers.
 */
public final class TryCatchLabel {
    private final Label label;

    public TryCatchLabel() {
        this.label = new Label();
    }

    /*package*/Label label() {
        return label;
    }
}
