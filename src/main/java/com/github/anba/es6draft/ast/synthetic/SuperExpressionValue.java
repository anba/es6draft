/**
 * Copyright (c) 2012-2014 André Bargull
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
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>12.2.4 The super Keyword
 * </ul>
 */
public final class SuperExpressionValue extends SuperExpression {
    public SuperExpressionValue(long beginPosition, long endPosition) {
        super(beginPosition, endPosition);
    }

    public SuperExpressionValue(long beginPosition, long endPosition, String name) {
        super(beginPosition, endPosition, name);
    }

    public SuperExpressionValue(long beginPosition, long endPosition, Expression expression) {
        super(beginPosition, endPosition, expression);
    }

    public SuperExpressionValue(long beginPosition, long endPosition, List<Expression> arguments) {
        super(beginPosition, endPosition, arguments);
    }

    @Override
    public Expression asValue() {
        return this;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
