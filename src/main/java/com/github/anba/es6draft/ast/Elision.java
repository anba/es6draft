/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.1 Primary Expressions</h2><br>
 * <h3>11.1.4 Array Initialiser</h3>
 * <ul>
 * <li>11.1.4.1 Array Literal
 * </ul>
 */
public class Elision extends Expression implements AssignmentElementItem {
    public Elision() {
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
