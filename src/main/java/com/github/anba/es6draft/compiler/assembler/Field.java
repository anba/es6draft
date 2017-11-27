/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

/**
 * Mutable value with reads/writes to a class field.
 */
public final class Field<V> implements MutableValue<V> {
    private final FieldName name;

    /**
     * Constructs a new field value.
     * 
     * @param name
     *            the field name
     */
    public Field(FieldName name) {
        this.name = name;
    }

    /**
     * Returns the field name
     * 
     * @return the field name
     */
    public FieldName getName() {
        return name;
    }

    @Override
    public void load(InstructionAssembler assembler) {
        assembler.get(name);
    }

    @Override
    public void store(InstructionAssembler assembler) {
        assembler.put(name);
    }
}
