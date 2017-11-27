/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;
import com.github.anba.es6draft.compiler.assembler.MethodTypeDescriptor;
import com.github.anba.es6draft.compiler.assembler.MutableValue;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;

/**
 * 
 */
abstract class OutlinedCodeVisitor extends CodeVisitor {
    private static final MethodTypeDescriptor OUTLINED_CALL = MethodTypeDescriptor.methodType(Type.INT_TYPE,
            Types.ExecutionContext, Types.Object_);
    private static final MethodTypeDescriptor OUTLINED_CALL_WITH_RESUME = MethodTypeDescriptor.methodType(Type.INT_TYPE,
            Types.ExecutionContext, Types.ResumptionPoint_, Types.Object_);

    private final Node node;
    private final boolean hasResume;

    protected OutlinedCodeVisitor(Node node, MethodCode method, CodeVisitor parent) {
        super(method, parent);
        this.node = node;
        this.hasResume = method.methodDescriptor.parameterType(1).equals(Types.ResumptionPoint_);
    }

    /**
     * Returns the outlined node.
     * 
     * @return the node
     */
    final Node getNode() {
        return node;
    }

    final boolean hasResume() {
        return hasResume;
    }

    /**
     * Creates a new method descriptor.
     * 
     * @param mv
     *            the parent code visitor
     * @return the method descriptor
     */
    protected static final MethodTypeDescriptor outlinedMethodDescriptor(CodeVisitor mv) {
        MethodTypeDescriptor rootDescriptor = mv.getRoot().getMethod().methodDescriptor;
        if (rootDescriptor.parameterCount() > 1 && rootDescriptor.parameterType(1).equals(Types.ResumptionPoint)) {
            return OUTLINED_CALL_WITH_RESUME;
        }
        return OUTLINED_CALL;
    }

    /**
     * Returns the actual parameter index.
     * 
     * @param index
     *            the relative parameter index
     * @return the actual parameter index
     */
    protected final int parameter(int index) {
        assert index >= 0;
        return hasResume ? index + 3 : index + 2;
    }

    private <T> MutableValue<T> arrayElementFromParameter(int index, Class<T[]> arrayType) {
        @SuppressWarnings("unchecked")
        Class<T> componentType = (Class<T>) arrayType.getComponentType();
        return arrayElement(getParameter(index, arrayType), 0, componentType);
    }

    @Override
    public void begin() {
        super.begin();
        setParameterName("cx", 0, Types.ExecutionContext);
        if (hasResume) {
            setParameterName("rp", 1, Types.ResumptionPoint_);
            setParameterName("completion", 2, Types.Object_);
        } else {
            setParameterName("completion", 1, Types.Object_);
        }
    }

    @Override
    protected final MutableValue<Object> createCompletionVariable() {
        return arrayElementFromParameter(hasResume ? 2 : 1, Object[].class);
    }

    @Override
    protected final MutableValue<ResumptionPoint> resumptionPoint() {
        assert hasResume;
        return arrayElementFromParameter(1, ResumptionPoint[].class);
    }

    @Override
    protected final void returnForSuspend() {
        store(resumptionPoint());
        iconst(-1);
        _return();
    }

    @Override
    protected final void returnForCompletion() {
        throw new AssertionError();
    }
}
