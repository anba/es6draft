/**
 * Copyright (c) 2012-2015 André Bargull
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
 * <li>12.3.4 Function Calls
 * </ul>
 */
public final class CallExpression extends Expression {
    private final Expression base;
    private final List<Expression> arguments;

    public CallExpression(long beginPosition, long endPosition, Expression base,
            List<Expression> arguments) {
        super(beginPosition, endPosition);
        this.base = base;
        this.arguments = arguments;
    }

    /**
     * Returns the function call's base expression.
     * 
     * @return the callee expression
     */
    public Expression getBase() {
        // TODO: Rename to 'callee'?
        return base;
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
