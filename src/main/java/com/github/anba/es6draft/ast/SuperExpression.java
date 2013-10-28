/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

import com.github.anba.es6draft.ast.synthetic.SuperExpressionValue;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>12.2.4 The super Keyword
 * </ul>
 */
public class SuperExpression extends LeftHandSideExpression {
    private String name;
    private Expression expression;
    private List<Expression> arguments;

    public SuperExpression(long beginPosition, long endPosition) {
        // new super()
        super(beginPosition, endPosition);
    }

    public SuperExpression(long beginPosition, long endPosition, String name) {
        // super.<name>
        super(beginPosition, endPosition);
        this.name = name;
    }

    public SuperExpression(long beginPosition, long endPosition, Expression expression) {
        // super[expression]
        super(beginPosition, endPosition);
        this.expression = expression;
    }

    public SuperExpression(long beginPosition, long endPosition, List<Expression> arguments) {
        // super(<arguments>)
        super(beginPosition, endPosition);
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public Expression getExpression() {
        return expression;
    }

    public List<Expression> getArguments() {
        return arguments;
    }

    @Override
    public Expression asValue() {
        if (name != null) {
            return new SuperExpressionValue(getBeginPosition(), getEndPosition(), name);
        } else if (expression != null) {
            return new SuperExpressionValue(getBeginPosition(), getEndPosition(), expression);
        } else if (arguments != null) {
            return new SuperExpressionValue(getBeginPosition(), getEndPosition(), arguments);
        } else {
            return new SuperExpressionValue(getBeginPosition(), getEndPosition());
        }
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
