/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.3 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>12.3.5 The super Keyword
 * </ul>
 */
public final class SuperNewExpression extends Expression {
    private final List<Expression> arguments;

    public SuperNewExpression(long beginPosition, long endPosition, List<Expression> arguments) {
        super(beginPosition, endPosition);
        this.arguments = arguments;
    }

    /**
     * Returns the list of arguments.
     * 
     * @return the arguments
     */
    public List<Expression> getArguments() {
        return arguments;
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
