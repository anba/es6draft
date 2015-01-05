/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * 
 */
abstract class ConstantPool {
    private final HashMap<Object, Integer> constantsMap = new HashMap<>(64);
    private ConstantPool next;
    private int integers = 0;
    private int longs = 0;
    private int floats = 0;
    private int doubles = 0;
    private int strings = 0;

    protected final Code code;
    private final int limit;

    protected ConstantPool(Code code, int limit) {
        assert 0 <= limit && limit <= Short.MAX_VALUE;
        this.code = code;
        this.limit = limit;
    }

    /**
     * Returns this pool's integer constants.
     * 
     * @return the integer constants
     */
    protected final Integer[] getIntegers() {
        Integer[] constants = new Integer[integers];
        for (Entry<Object, Integer> entry : constantsMap.entrySet()) {
            if (entry.getKey() instanceof Integer) {
                constants[entry.getValue()] = (Integer) entry.getKey();
            }
        }
        return constants;
    }

    /**
     * Returns this pool's long constants.
     * 
     * @return the long constants
     */
    protected final Long[] getLongs() {
        Long[] constants = new Long[longs];
        for (Entry<Object, Integer> entry : constantsMap.entrySet()) {
            if (entry.getKey() instanceof Long) {
                constants[entry.getValue()] = (Long) entry.getKey();
            }
        }
        return constants;
    }

    /**
     * Returns this pool's float constants.
     * 
     * @return the float constants
     */
    protected final Float[] getFloats() {
        Float[] constants = new Float[floats];
        for (Entry<Object, Integer> entry : constantsMap.entrySet()) {
            if (entry.getKey() instanceof Float) {
                constants[entry.getValue()] = (Float) entry.getKey();
            }
        }
        return constants;
    }

    /**
     * Returns this pool's double constants.
     * 
     * @return the double constants
     */
    protected final Double[] getDoubles() {
        Double[] constants = new Double[doubles];
        for (Entry<Object, Integer> entry : constantsMap.entrySet()) {
            if (entry.getKey() instanceof Double) {
                constants[entry.getValue()] = (Double) entry.getKey();
            }
        }
        return constants;
    }

    /**
     * Returns this pool's string constants.
     * 
     * @return the string constants
     */
    protected final String[] getStrings() {
        String[] constants = new String[strings];
        for (Entry<Object, Integer> entry : constantsMap.entrySet()) {
            if (entry.getKey() instanceof String) {
                constants[entry.getValue()] = (String) entry.getKey();
            }
        }
        return constants;
    }

    private boolean isConstantPoolFull() {
        return constantsMap.size() >= limit;
    }

    private int getConstantIndex(Object cst) {
        Integer index = constantsMap.get(cst);
        return index != null ? index : -1;
    }

    private void putConstant(Object cst, int index) {
        constantsMap.put(cst, index);
    }

    private ConstantPool getNext() {
        if (next == null) {
            next = newConstantPool();
        }
        return next;
    }

    public final void iconst(InstructionAssembler assembler, Integer cst) {
        int index = getConstantIndex(cst);
        if (index < 0) {
            if (isConstantPoolFull()) {
                getNext().iconst(assembler, cst);
                return;
            }
            putConstant(cst, index = integers++);
        }
        iconst(assembler, cst, index);
    }

    public final void lconst(InstructionAssembler assembler, Long cst) {
        int index = getConstantIndex(cst);
        if (index < 0) {
            if (isConstantPoolFull()) {
                getNext().lconst(assembler, cst);
                return;
            }
            putConstant(cst, index = longs++);
        }
        lconst(assembler, cst, index);
    }

    public final void fconst(InstructionAssembler assembler, Float cst) {
        int index = getConstantIndex(cst);
        if (index < 0) {
            if (isConstantPoolFull()) {
                getNext().fconst(assembler, cst);
                return;
            }
            putConstant(cst, index = floats++);
        }
        fconst(assembler, cst, index);
    }

    public final void dconst(InstructionAssembler assembler, Double cst) {
        int index = getConstantIndex(cst);
        if (index < 0) {
            if (isConstantPoolFull()) {
                getNext().dconst(assembler, cst);
                return;
            }
            putConstant(cst, index = doubles++);
        }
        dconst(assembler, cst, index);
    }

    public final void aconst(InstructionAssembler assembler, String cst) {
        int index = getConstantIndex(cst);
        if (index < 0) {
            if (isConstantPoolFull()) {
                getNext().aconst(assembler, cst);
                return;
            }
            putConstant(cst, index = strings++);
        }
        aconst(assembler, cst, index);
    }

    /**
     * Close this constant pool
     */
    protected abstract void close();

    /**
     * Create a new constant pool when this pool's limit has been exceeded.
     * 
     * @return the new constant pool
     */
    protected abstract ConstantPool newConstantPool();

    /**
     * Load the indexed integer constant {@code cst} for the given method.
     * 
     * @param assembler
     *            the instruction assembler
     * @param cst
     *            the integer constant
     * @param index
     *            the constant index
     */
    protected abstract void iconst(InstructionAssembler assembler, Integer cst, int index);

    /**
     * Load the indexed long constant {@code cst} for the given method.
     * 
     * @param assembler
     *            the instruction assembler
     * @param cst
     *            the long constant
     * @param index
     *            the constant index
     */
    protected abstract void lconst(InstructionAssembler assembler, Long cst, int index);

    /**
     * Load the indexed float constant {@code cst} for the given method.
     * 
     * @param assembler
     *            the instruction assembler
     * @param cst
     *            the float constant
     * @param index
     *            the constant index
     */
    protected abstract void fconst(InstructionAssembler assembler, Float cst, int index);

    /**
     * Load the indexed double constant {@code cst} for the given method.
     * 
     * @param assembler
     *            the instruction assembler
     * @param cst
     *            the double constant
     * @param index
     *            the constant index
     */
    protected abstract void dconst(InstructionAssembler assembler, Double cst, int index);

    /**
     * Load the indexed string constant {@code cst} for the given method.
     * 
     * @param assembler
     *            the instruction assembler
     * @param cst
     *            the string constant
     * @param index
     *            the constant index
     */
    protected abstract void aconst(InstructionAssembler assembler, String cst, int index);
}
