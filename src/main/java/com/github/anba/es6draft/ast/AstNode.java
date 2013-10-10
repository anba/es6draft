/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Base class for all abstract-syntax-tree nodes
 */
abstract class AstNode implements Node {
    private final long sourcePosition;

    protected AstNode(long sourcePosition) {
        this.sourcePosition = sourcePosition;
    }

    @Override
    public int getLine() {
        return (int) sourcePosition;
    }

    @Override
    public int getColumn() {
        return (int) (sourcePosition >>> 32);
    }

    @Override
    public long getSourcePosition() {
        return sourcePosition;
    }

    @Override
    public abstract <R, V> R accept(NodeVisitor<R, V> visitor, V value);
}
