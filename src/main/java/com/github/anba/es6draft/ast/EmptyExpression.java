/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1><br>
 * <ul>
 * <li>14.2 Arrow Function Definitions
 * </ul>
 */
public class EmptyExpression extends Expression {
    public EmptyExpression(long beginPosition, long endPosition) {
        super(beginPosition, endPosition);
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        throw new IllegalStateException();
    }
}
