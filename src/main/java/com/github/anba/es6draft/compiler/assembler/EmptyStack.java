/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

/**
 * {@link Stack} sub-class to make every operation a no-op.
 */
public final class EmptyStack extends Stack {
    public EmptyStack(Variables variables) {
        super(variables);
    }

    @Override
    protected Type peek() {
        return Type.VOID_TYPE;
    }

    @Override
    protected Type peek(int n) {
        return Type.VOID_TYPE;
    }

    @Override
    protected Type pop(int size) {
        return Type.VOID_TYPE;
    }

    @Override
    protected void pop(Type type) {
    }

    @Override
    protected void push(Type type) {
    }

    @Override
    protected void jmp(Jump jump) {
    }

    @Override
    protected void label(Jump jump) {
    }
}
