/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Constant pool abstraction to handle the Java bytecode limitation on constant entries per class.
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

    private ConstantPool next() {
        if (next == null) {
            next = newConstantPool();
        }
        return next;
    }

    /**
     * Adds and loads the given integer constant.
     * 
     * @param assembler
     *            the instruction assembler
     * @param cst
     *            the integer constant
     */
    public final void iconst(InstructionAssembler assembler, Integer cst) {
        ConstantPool cp = this;
        int index;
        for (; (index = cp.constantsMap.getOrDefault(cst, -1)) < 0; cp = cp.next()) {
            if (cp.constantsMap.size() < cp.limit) {
                cp.constantsMap.put(cst, index = cp.integers++);
                break;
            }
        }
        cp.iconst(assembler, cst, index);
    }

    /**
     * Adds and loads the given long constant.
     * 
     * @param assembler
     *            the instruction assembler
     * @param cst
     *            the long constant
     */
    public final void lconst(InstructionAssembler assembler, Long cst) {
        ConstantPool cp = this;
        int index;
        for (; (index = cp.constantsMap.getOrDefault(cst, -1)) < 0; cp = cp.next()) {
            if (cp.constantsMap.size() < cp.limit) {
                cp.constantsMap.put(cst, index = cp.longs++);
                break;
            }
        }
        cp.lconst(assembler, cst, index);
    }

    /**
     * Adds and loads the given float constant.
     * 
     * @param assembler
     *            the instruction assembler
     * @param cst
     *            the float constant
     */
    public final void fconst(InstructionAssembler assembler, Float cst) {
        ConstantPool cp = this;
        int index;
        for (; (index = cp.constantsMap.getOrDefault(cst, -1)) < 0; cp = cp.next()) {
            if (cp.constantsMap.size() < cp.limit) {
                cp.constantsMap.put(cst, index = cp.floats++);
                break;
            }
        }
        cp.fconst(assembler, cst, index);
    }

    /**
     * Adds and loads the given double constant.
     * 
     * @param assembler
     *            the instruction assembler
     * @param cst
     *            the double constant
     */
    public final void dconst(InstructionAssembler assembler, Double cst) {
        ConstantPool cp = this;
        int index;
        for (; (index = cp.constantsMap.getOrDefault(cst, -1)) < 0; cp = cp.next()) {
            if (cp.constantsMap.size() < cp.limit) {
                cp.constantsMap.put(cst, index = cp.doubles++);
                break;
            }
        }
        cp.dconst(assembler, cst, index);
    }

    /**
     * Adds and loads the given string constant.
     * 
     * @param assembler
     *            the instruction assembler
     * @param cst
     *            the string constant
     */
    public final void aconst(InstructionAssembler assembler, String cst) {
        ConstantPool cp = this;
        int index;
        for (; (index = cp.constantsMap.getOrDefault(cst, -1)) < 0; cp = cp.next()) {
            if (cp.constantsMap.size() < cp.limit) {
                cp.constantsMap.put(cst, index = cp.strings++);
                break;
            }
        }
        cp.aconst(assembler, cst, index);
    }

    /**
     * Closes this constant pool.
     */
    protected abstract void close();

    /**
     * Creates a new constant pool when this pool's limit has been exceeded.
     * 
     * @return the new constant pool
     */
    protected abstract ConstantPool newConstantPool();

    /**
     * Loads the indexed integer constant {@code cst} for the given method.
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
     * Loads the indexed long constant {@code cst} for the given method.
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
     * Loads the indexed float constant {@code cst} for the given method.
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
     * Loads the indexed double constant {@code cst} for the given method.
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
     * Loads the indexed string constant {@code cst} for the given method.
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
