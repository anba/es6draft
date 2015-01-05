/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Extension: Array and Generator Comprehension
 */
public final class ArrayComprehension extends ArrayInitializer {
    private final Comprehension comprehension;

    public ArrayComprehension(long beginPosition, long endPosition, Comprehension comprehension) {
        super(beginPosition, endPosition);
        this.comprehension = comprehension;
    }

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
