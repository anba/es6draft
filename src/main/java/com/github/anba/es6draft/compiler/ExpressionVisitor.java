/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static java.util.Collections.emptySet;

import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.Scope;
import com.github.anba.es6draft.ast.ScopedNode;
import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * 
 */
abstract class ExpressionVisitor extends InstructionVisitor {
    private static class Fields {
        static final FieldDesc Null_NULL = FieldDesc.create(FieldType.Static, Types.Null, "NULL",
                Types.Null);

        static final FieldDesc Undefined_UNDEFINED = FieldDesc.create(FieldType.Static,
                Types.Undefined, "UNDEFINED", Types.Undefined);
    }

    private final boolean strict;
    private final boolean globalCode;
    private final Variable<ExecutionContext> executionContext;
    private Scope scope;
    // tail-call support
    private Set<Expression> tail = emptySet();

    protected ExpressionVisitor(MethodVisitor mv, String methodName, Type methodDescriptor,
            boolean strict, boolean globalCode) {
        super(mv, methodName, methodDescriptor);
        this.strict = strict;
        this.globalCode = globalCode;
        this.executionContext = reserveFixedSlot(0, ExecutionContext.class);
    }

    /**
     * &#x2205; → cx
     */
    void loadExecutionContext() {
        load(executionContext);
    }

    /**
     * &#x2205; → undefined
     */
    void loadUndefined() {
        get(Fields.Undefined_UNDEFINED);
    }

    /**
     * &#x2205; → null
     */
    void loadNull() {
        get(Fields.Null_NULL);
    }

    boolean isStrict() {
        return strict;
    }

    boolean isGlobalCode() {
        return globalCode;
    }

    Scope getScope() {
        return scope;
    }

    void setScope(Scope scope) {
        this.scope = scope;
    }

    Scope enterScope(ScopedNode node) {
        assert node.getScope().getParent() == this.scope;
        return this.scope = node.getScope();
    }

    Scope exitScope() {
        return scope = scope.getParent();
    }

    void lineInfo(Node node) {
        lineInfo(node.getLine());
    }

    boolean isTailCall(Expression expr) {
        return tail.contains(expr);
    }

    void setTailCall(Set<Expression> tail) {
        assert tail != null;
        this.tail = tail;
    }
}
