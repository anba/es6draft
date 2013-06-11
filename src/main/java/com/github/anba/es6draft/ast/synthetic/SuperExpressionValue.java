/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import java.util.List;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.SuperExpression;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.2 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>11.2.4 The super Keyword
 * </ul>
 */
public class SuperExpressionValue extends SuperExpression {
    public SuperExpressionValue() {
        super();
    }

    public SuperExpressionValue(String name) {
        super(name);
    }

    public SuperExpressionValue(Expression expression) {
        super(expression);
    }

    public SuperExpressionValue(List<Expression> arguments) {
        super(arguments);
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}