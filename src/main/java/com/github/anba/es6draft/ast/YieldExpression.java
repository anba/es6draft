/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.4 Generator Function Definitions
 * </ul>
 */
public final class YieldExpression extends Expression {
    private final boolean delegatedYield;
    private final Expression expression;

    public YieldExpression(long beginPosition, long endPosition, boolean delegatedYield,
            Expression expression) {
        super(beginPosition, endPosition);
        this.delegatedYield = delegatedYield;
        this.expression = expression;
    }

    /**
     * Returns {@code true} if this yield expression represents a delegating yield expression.
     * 
     * @return {@code true} if a delegating yield expression
     */
    public boolean isDelegatedYield() {
        return delegatedYield;
    }

    /**
     * Returns the expression of this yield expression or {@code null} if not present.
     * 
     * @return the expression
     */
    public Expression getExpression() {
        return expression;
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
