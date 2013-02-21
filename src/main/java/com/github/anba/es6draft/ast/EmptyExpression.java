/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>13 Functions and Generators</h1><br>
 * <ul>
 * <li>13.2 Arrow Function Definitions
 * </ul>
 */
public class EmptyExpression extends Expression {
    public EmptyExpression() {
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        throw new IllegalStateException();
    }
}
