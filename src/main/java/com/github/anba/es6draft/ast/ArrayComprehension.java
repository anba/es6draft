/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2><br>
 * <h3>12.2.4 Array Initializer</h3>
 * <ul>
 * <li>12.2.4.2 Array Comprehension
 * </ul>
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
}
