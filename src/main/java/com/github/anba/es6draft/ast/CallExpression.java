/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.2 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>11.2.3 Function Calls
 * </ul>
 */
public class CallExpression extends Expression {
    private Expression base;
    private List<Expression> arguments;

    public CallExpression(Expression base, List<Expression> arguments) {
        this.base = base;
        this.arguments = arguments;
    }

    public Expression getBase() {
        return base;
    }

    public List<Expression> getArguments() {
        return arguments;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
