/**
 * Copyright (c) 2012-2014 André Bargull
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
    private boolean delegatedYield;
    private Expression expression;

    public YieldExpression(long beginPosition, long endPosition, boolean delegatedYield,
            Expression expression) {
        super(beginPosition, endPosition);
        this.delegatedYield = delegatedYield;
        this.expression = expression;
    }

    public boolean isDelegatedYield() {
        return delegatedYield;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
