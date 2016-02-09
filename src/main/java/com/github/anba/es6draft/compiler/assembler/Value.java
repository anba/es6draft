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
@FunctionalInterface
public interface Value<V> {
    /**
     * Loads this value onto the stack.
     * 
     * @param assembler
     *            the instruction assembler
     */
    void load(InstructionAssembler assembler);
}
