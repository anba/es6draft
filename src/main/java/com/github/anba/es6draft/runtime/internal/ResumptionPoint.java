/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

/**
 * Method execution resumption point.
 */
public final class ResumptionPoint {
    private final Object[] stack;
    private final Object[] locals;
    private final int offset;
    // Null for generator frames, non-null for other method frames.
    private final ResumptionPoint next;

    private ResumptionPoint(Object[] stack, Object[] locals, int offset, ResumptionPoint next) {
        assert stack != null && offset >= 0;
        this.stack = stack;
        this.locals = locals;
        this.offset = offset;
        this.next = next;
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
        return new ResumptionPoint(stack, locals, offset, null);
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
     * @param next
     *            the next resumption point
     * @return the new resumption point
     */
    public static ResumptionPoint create(Object[] stack, Object[] locals, int offset, ResumptionPoint next) {
        return new ResumptionPoint(stack, locals, offset, next);
    }

    /**
     * Returns the suspend value.
     * 
     * @return the suspend value
     */
    public Object getSuspendValue() {
        if (next == null) {
            assert stack.length > 0;
            return stack[stack.length - 1];
        }
        return next.getSuspendValue();
    }

    /**
     * Sets the resume value.
     * 
     * @param value
     *            the resume value
     */
    public void setResumeValue(Object value) {
        if (next == null) {
            assert stack.length > 0;
            stack[stack.length - 1] = value;
        } else {
            next.setResumeValue(value);
        }
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
