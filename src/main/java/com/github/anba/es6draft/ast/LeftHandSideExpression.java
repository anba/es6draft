/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <ul>
 * <li>12.2 Left-Hand-Side Expressions
 * </ul>
 */
public abstract class LeftHandSideExpression extends Expression {
    protected LeftHandSideExpression(long sourcePosition) {
        super(sourcePosition);
    }

    @Override
    public abstract Expression asValue();
}
