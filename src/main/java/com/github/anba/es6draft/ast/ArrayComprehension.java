/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Extension: Array and Generator Comprehension
 */
public final class ArrayComprehension extends Expression {
    private final Comprehension comprehension;

    public ArrayComprehension(long beginPosition, long endPosition, Comprehension comprehension) {
        super(beginPosition, endPosition);
        this.comprehension = comprehension;
    }

    /**
     * Returns the comprehension part of this array comprehension.
     * 
     * @return the comprehension part
     */
    public Comprehension getComprehension() {
        return comprehension;
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
