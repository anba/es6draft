/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.IntNodeVisitor;
import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.VoidNodeVisitor;

/**
 * {@link Expression} as an external Java method.
 */
public final class ExpressionMethod extends Expression implements SyntheticNode {
    private final Expression expression;
    private boolean resumePoint;

    public ExpressionMethod(Expression expression) {
        super(expression.getBeginPosition(), expression.getEndPosition());
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public boolean hasResumePoint() {
        return resumePoint;
    }

    @Override
    public void setResumePoint(boolean resumePoint) {
        this.resumePoint = resumePoint;
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
    public boolean isParenthesized() {
        return expression.isParenthesized();
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
