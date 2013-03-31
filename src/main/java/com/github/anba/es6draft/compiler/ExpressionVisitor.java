/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.CallExpression;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.Scope;
import com.github.anba.es6draft.ast.ScopedNode;

/**
 * 
 */
abstract class ExpressionVisitor extends InstructionVisitor {
    private final boolean strict;
    private final boolean globalCode;
    private Scope scope;
    // tail-call support
    private Set<CallExpression> tail = null;

    protected ExpressionVisitor(MethodVisitor mv, String methodName, Type methodDescriptor,
            boolean strict, boolean globalCode, boolean completionValue) {
        super(mv, methodName, methodDescriptor);
        this.strict = strict;
        this.globalCode = globalCode;
        reserveFixedSlot(0, Types.ExecutionContext);
    }

    void loadExecutionContext() {
        load(0, Types.ExecutionContext);
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

    boolean isTailCall(CallExpression expr) {
        return tail != null && tail.contains(expr);
    }

    void setTailCall(CallExpression expr) {
        if (tail == null) {
            tail = new HashSet<>();
        }
        tail.add(expr);
    }
}