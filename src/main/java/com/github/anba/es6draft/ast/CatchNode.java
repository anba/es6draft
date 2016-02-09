/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.scope.BlockScope;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.15 The try Statement
 * </ul>
 */
public final class CatchNode extends AstNode implements CatchClause {
    private final BlockScope scope;
    private final Binding catchParameter;
    private final BlockStatement catchBlock;

    public CatchNode(long beginPosition, long endPosition, BlockScope scope, Binding catchParameter,
            BlockStatement catchBlock) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.catchParameter = catchParameter;
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
