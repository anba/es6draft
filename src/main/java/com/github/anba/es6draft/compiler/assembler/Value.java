/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

/**
 *
 */
public abstract class Value<V> {
    protected Value() {
    }

    protected abstract void load(InstructionAssembler assembler);
}
