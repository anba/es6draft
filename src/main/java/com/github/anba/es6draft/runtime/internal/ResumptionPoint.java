/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

/**
 * Method execution resumption point
 */
public final class ResumptionPoint {
    private final Object[] stack;
    private final Object[] locals;
    private final int offset;

    private ResumptionPoint(Object[] stack, Object[] locals, int offset) {
        assert stack != null && locals != null && offset >= 0;
        this.stack = stack;
        this.locals = locals;
        this.offset = offset;
    }

    /**
     * Creates a new {@link ResumptionPoint} object.
     * 
     * @param stack
     *            the current stack
     * @param locals
     *            the current locals
     * @param offset
     *            the resumption point offset
     * @return the new resumption point
     */
    public static ResumptionPoint create(Object[] stack, Object[] locals, int offset) {
        return new ResumptionPoint(stack, locals, offset);
    }

    /**
     * Returns the stack top value.
     * 
     * @return the stack top
     */
    public Object getStackTop() {
        assert stack.length > 0;
        return stack[stack.length - 1];
    }

    /**
     * Returns the stack top value.
     * 
     * @param value
     *            the new stack top
     */
    public void setStackTop(Object value) {
        assert stack.length > 0;
        stack[stack.length - 1] = value;
    }

    /**
     * Returns the stored stack.
     * 
     * @return the stored stack
     */
    public Object[] getStack() {
        return stack;
    }

    /**
     * Returns the stored locals.
     * 
     * @return the stored locals
     */
    public Object[] getLocals() {
        return locals;
    }

    /**
     * Returns the stored offset.
     * 
     * @return the execution offset
     */
    public int getOffset() {
        return offset;
    }
}
