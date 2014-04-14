/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>12 ECMAScript Language: Expressions</h1>
 * <ul>
 * <li>12.15 Comma Operator ( , )
 * </ul>
 */
public final class CommaExpression extends Expression {
    private List<Expression> operands;

    public CommaExpression(List<Expression> operands) {
        super(first(operands).getBeginPosition(), last(operands).getEndPosition());
        this.operands = operands;
    }

    public List<Expression> getOperands() {
        return operands;
    }

    public void setOperands(List<Expression> operands) {
        this.operands = operands;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }

    private static Expression first(List<Expression> elements) {
        assert !elements.isEmpty();
        return elements.get(0);
    }

    private static Expression last(List<Expression> elements) {
        assert !elements.isEmpty();
        return elements.get(elements.size() - 1);
    }
}
