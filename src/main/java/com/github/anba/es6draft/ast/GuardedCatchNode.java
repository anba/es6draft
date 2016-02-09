/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.scope.BlockScope;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.15 The try Statement</h2>
 * <ul>
 * <li>Extension: 'catch-if' node
 * </ul>
 */
public final class GuardedCatchNode extends AstNode implements CatchClause {
    private final BlockScope scope;
    private final Binding catchParameter;
    private final BlockStatement catchBlock;
    private final Expression guard;

    public GuardedCatchNode(long beginPosition, long endPosition, BlockScope scope, Binding catchParameter,
            Expression guard, BlockStatement catchBlock) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.catchParameter = catchParameter;
        this.guard = guard;
        this.catchBlock = catchBlock;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    @Override
    public Binding getCatchParameter() {
        return catchParameter;
    }

    public Expression getGuard() {
        return guard;
    }

    @Override
    public BlockStatement getCatchBlock() {
        return catchBlock;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        visitor.visit(this, value);
    }
}
