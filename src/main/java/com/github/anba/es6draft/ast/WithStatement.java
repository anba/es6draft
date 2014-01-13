/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.10 The with Statement
 * </ul>
 */
public class WithStatement extends Statement implements ScopedNode {
    private BlockScope scope;
    private Expression expression;
    private Statement statement;

    public WithStatement(long beginPosition, long endPosition, BlockScope scope,
            Expression expression, Statement statement) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.expression = expression;
        this.statement = statement;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    public Expression getExpression() {
        return expression;
    }

    public Statement getStatement() {
        return statement;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
