/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Extension: Async Function Expression
 */
public final class AwaitExpression extends Expression {
    private final Expression expression;

    public AwaitExpression(long beginPosition, long endPosition, Expression expression) {
        super(beginPosition, endPosition);
        this.expression = expression;
    }

    /**
     * Returns the expression part of this <code>AwaitExpression</code> node.
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
