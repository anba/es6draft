/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.NodeVisitor;

/**
 * {@link Expression} as an external Java method
 */
public class ExpressionMethod extends Expression {
    private Expression expression;

    public ExpressionMethod(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public int getLine() {
        return expression.getLine();
    }

    @Override
    public void addParentheses() {
        expression.addParentheses();
    }

    @Override
    public int getParentheses() {
        return expression.getParentheses();
    }

    @Override
    public boolean isParenthesised() {
        return expression.isParenthesised();
    }

    @Override
    public Expression asValue() {
        Expression valueExpression = expression.asValue();
        if (valueExpression != expression) {
            return new ExpressionMethod(valueExpression);
        }
        return this;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
