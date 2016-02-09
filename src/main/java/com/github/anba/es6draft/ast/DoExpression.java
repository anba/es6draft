/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Extension: {@code do}-expressions
 */
public final class DoExpression extends Expression {
    private final BlockStatement statement;
    private final boolean yieldOrAwait;
    private boolean completion = true;

    public DoExpression(long beginPosition, long endPosition, BlockStatement statement, boolean yieldOrAwait) {
        super(beginPosition, endPosition);
        this.statement = statement;
        this.yieldOrAwait = yieldOrAwait;
    }

    /**
     * Returns the statement block.
     * 
     * @return the statement
     */
    public BlockStatement getStatement() {
        return statement;
    }

    /**
     * Returns {@code true} if a {@code yield} or {@code await} expression is nested in this expression.
     * 
     * @return {@code true} if this expression contains a {@code yield} or {@code await} expression
     */
    public boolean hasYieldOrAwait() {
        return yieldOrAwait;
    }

    @Override
    public Expression emptyCompletion() {
        completion = false;
        return this;
    }

    /**
     * Returns {@code true} if the completion value is used.
     * 
     * @return {@code true} if the completion value is used
     * @see #emptyCompletion()
     */
    public boolean hasCompletion() {
        return completion;
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
