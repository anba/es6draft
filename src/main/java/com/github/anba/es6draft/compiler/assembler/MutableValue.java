/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

/**
 *
 */
public interface MutableValue<V> extends Value<V> {
    /**
     * Stores a value from the stack into this mutable value.
     * 
     * @param assembler
     *            the instruction assembler
     */
    void store(InstructionAssembler assembler);

    /**
     * Stores a value into this mutable value.
     * 
     * @param assembler
     *            the instruction assembler
     * @param value
     *            the value
     */
    default void store(InstructionAssembler assembler, Value<? extends V> value) {
        value.load(assembler);
        store(assembler);
    }
}
