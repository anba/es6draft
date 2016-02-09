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
public final class Field<V> implements Value<V> {
    private final FieldName name;

    public Field(FieldName name) {
        this.name = name;
    }

    public FieldName getName() {
        return name;
    }

    @Override
    public void load(InstructionAssembler assembler) {
        assembler.get(name);
    }
}
