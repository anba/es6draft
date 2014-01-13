/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.TailCallNodes;
import static java.util.Collections.emptySet;

import java.util.Collections;
import java.util.Set;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.Scope;
import com.github.anba.es6draft.ast.ScopedNode;
import com.github.anba.es6draft.compiler.Code.MethodCode;
import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * 
 */
abstract class ExpressionVisitor extends InstructionVisitor {
    private static final class Fields {
        static final FieldDesc Null_NULL = FieldDesc.create(FieldType.Static, Types.Null, "NULL",
                Types.Null);

        static final FieldDesc Undefined_UNDEFINED = FieldDesc.create(FieldType.Static,
                Types.Undefined, "UNDEFINED", Types.Undefined);
    }

    private static final int CONTEXT_SLOT = 0;

    private final boolean strict;
    private final boolean globalCode;
    private final boolean syntheticMethods;
    private Variable<ExecutionContext> executionContext;
    private Scope scope;
    // tail-call support
    private boolean hasTailCalls = false;
    private Set<Expression> tailCallNodes = emptySet();

    protected ExpressionVisitor(MethodCode method, ExpressionVisitor parent) {
        super(method);
        this.strict = parent.isStrict();
        this.globalCode = parent.isGlobalCode();
        this.syntheticMethods = true;
    }

    protected ExpressionVisitor(MethodCode method, boolean strict, boolean globalCode,
            boolean syntheticMethods) {
        super(method);
        this.strict = strict;
        this.globalCode = globalCode;
        this.syntheticMethods = syntheticMethods;
    }

    @Override
    public void begin() {
        super.begin();
        this.executionContext = getParameter(CONTEXT_SLOT, ExecutionContext.class);
    }

    /**
     * Update additional state information after nested {@link ExpressionVisitor} has finished its
     * pass
     */
    void updateInfo(ExpressionVisitor nested) {
        hasTailCalls |= nested.hasTailCalls;
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

    boolean hasSyntheticMethods() {
        return syntheticMethods;
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
        lineInfo(node.getBeginLine());
    }

    void enterTailCallPosition(Expression expr) {
        if (isStrict()) {
            // Tail calls are only enabled in strict-mode code [14.6.1 Tail Position, step 2]
            this.tailCallNodes = TailCallNodes(expr);
        }
    }

    final void exitTailCallPosition() {
        this.tailCallNodes = Collections.emptySet();
    }

    final boolean hasTailCalls() {
        return hasTailCalls;
    }

    final boolean isTailCall(Expression expr) {
        boolean isTaillCall = tailCallNodes.contains(expr);
        hasTailCalls |= isTaillCall;
        return isTaillCall;
    }
}
