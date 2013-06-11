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
 * <h1>11 Expressions</h1><br>
 * <h2>11.2 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>11.2.4 The super Keyword
 * </ul>
 */
public class SuperExpression extends LeftHandSideExpression {
    private String name;
    private Expression expression;
    private List<Expression> arguments;

    public SuperExpression() {
        // new super()
    }

    public SuperExpression(String name) {
        // super.<name>
        this.name = name;
    }

    public SuperExpression(Expression expression) {
        // super[expression]
        this.expression = expression;
    }

    public SuperExpression(List<Expression> arguments) {
        // super(<arguments>)
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
            return new SuperExpressionValue(name);
        }
        if (expression != null) {
            return new SuperExpressionValue(expression);
        }
        if (arguments != null) {
            return new SuperExpressionValue(arguments);
        }
        return new SuperExpressionValue();
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
