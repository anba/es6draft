/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Base class for all abstract-syntax-tree nodes
 */
abstract class AstNode implements Node {
    private final long beginPosition, endPosition;

    protected AstNode(long beginPosition, long endPosition) {
        this.beginPosition = beginPosition;
        this.endPosition = endPosition;
    }

    @Override
    public int getBeginLine() {
        return (int) beginPosition;
    }

    @Override
    public int getBeginColumn() {
        return (int) (beginPosition >>> 32);
    }

    @Override
    public long getBeginPosition() {
        return beginPosition;
    }

    @Override
    public int getEndLine() {
        return (int) endPosition;
    }

    @Override
    public int getEndColumn() {
        return (int) (endPosition >>> 32);
    }

    @Override
    public long getEndPosition() {
        return endPosition;
    }

    @Override
    public abstract <R, V> R accept(NodeVisitor<R, V> visitor, V value);

    @Override
    public abstract <V> int accept(IntNodeVisitor<V> visitor, V value);

    @Override
    public abstract <V> void accept(VoidNodeVisitor<V> visitor, V value);
}
