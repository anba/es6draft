/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

/**
 * Concrete {@link ConstantPool} implementation which handles all constant values as inline
 * constants, i.e. the constant value is directly written into the current class.
 */
final class InlineConstantPool extends ConstantPool {
    private static final int INLINE_CONSTANTS_LIMIT = 0x2000;

    InlineConstantPool(Code code) {
        super(code, INLINE_CONSTANTS_LIMIT);
    }

    @Override
    protected void close() {
        // empty
    }

    @Override
    protected ConstantPool newConstantPool() {
        return code.getSharedConstantPool();
    }

    @Override
    protected void iconst(InstructionAssembler assembler, Integer cst, int index) {
        assembler.ldc(cst);
    }

    @Override
    protected void lconst(InstructionAssembler assembler, Long cst, int index) {
        assembler.ldc(cst);
    }

    @Override
    protected void fconst(InstructionAssembler assembler, Float cst, int index) {
        assembler.ldc(cst);
    }

    @Override
    protected void dconst(InstructionAssembler assembler, Double cst, int index) {
        assembler.ldc(cst);
    }

    @Override
    protected void aconst(InstructionAssembler assembler, String cst, int index) {
        assembler.ldc(cst);
    }
}
