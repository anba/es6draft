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
        SuperExpressionValue value;
        if (name != null) {
            value = new SuperExpressionValue(name);
        } else if (expression != null) {
            value = new SuperExpressionValue(expression);
        } else if (arguments != null) {
            value = new SuperExpressionValue(arguments);
        } else {
            value = new SuperExpressionValue();
        }
        value.setLine(getLine());
        return value;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
