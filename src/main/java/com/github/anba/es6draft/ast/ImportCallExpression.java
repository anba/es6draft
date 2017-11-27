/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Extension: dynamic import
 */
public final class ImportCallExpression extends Expression {
    private final Expression argument;

    public ImportCallExpression(long beginPosition, long endPosition, Expression argument) {
        super(beginPosition, endPosition);
        this.argument = argument;
    }

    /**
     * Returns the argument expression.
     * 
     * @return the arguments
     */
    public Expression getArgument() {
        return argument;
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
