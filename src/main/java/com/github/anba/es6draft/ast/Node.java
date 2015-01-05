/**
 * Copyright (c) 2012-2015 André Bargull
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
     * Returns the begin line position.
     * 
     * @return the begin line position
     */
    int getBeginLine();

    /**
     * Returns the begin column position.
     * 
     * @return the begin column position
     */
    int getBeginColumn();

    /**
     * Returns the begin position.
     * 
     * @return the begin position
     */
    long getBeginPosition();

    /**
     * Returns the end line position.
     * 
     * @return the end line position
     */
    int getEndLine();

    /**
     * Returns the end column position.
     * 
     * @return the end column position
     */
    int getEndColumn();

    /**
     * Returns the end position.
     * 
     * @return the end position
     */
    long getEndPosition();

    /**
     * Visitor pattern {@code accept()} method.
     * 
     * @param <R>
     *            the return value type
     * @param <V>
     *            the value object type
     * @param visitor
     *            the visitor instance
     * @param value
     *            the value object
     * @return the return value from the visitor
     */
    <R, V> R accept(NodeVisitor<R, V> visitor, V value);

    <V> int accept(IntNodeVisitor<V> visitor, V value);

    <V> void accept(VoidNodeVisitor<V> visitor, V value);
}
