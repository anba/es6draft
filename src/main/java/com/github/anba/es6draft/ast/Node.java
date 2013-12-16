/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Base interface for all abstract-syntax-tree nodes
 */
public interface Node {
    /**
     * Returns the begin line position
     */
    int getBeginLine();

    /**
     * Returns the begin column position
     */
    int getBeginColumn();

    /**
     * Returns the begin position
     */
    long getBeginPosition();

    /**
     * Returns the end line position
     */
    int getEndLine();

    /**
     * Returns the end column position
     */
    int getEndColumn();

    /**
     * Returns the end position
     */
    long getEndPosition();

    /**
     * Visitor pattern {@code accept()} method
     */
    <R, V> R accept(NodeVisitor<R, V> visitor, V value);
}
