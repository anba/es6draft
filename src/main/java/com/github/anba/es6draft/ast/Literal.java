/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2>
 * <ul>
 * <li>12.2.3 Literals
 * </ul>
 */
public abstract class Literal extends Expression {
    protected Literal(long beginPosition, long endPosition) {
        super(beginPosition, endPosition);
    }

    @Override
    public Expression emptyCompletion() {
        return EmptyExpression.EMPTY;
    }
}
