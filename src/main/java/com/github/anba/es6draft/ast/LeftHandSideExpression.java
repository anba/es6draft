/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <ul>
 * <li>12.3 Left-Hand-Side Expressions
 * </ul>
 */
public abstract class LeftHandSideExpression extends Expression {
    protected LeftHandSideExpression(long beginPosition, long endPosition) {
        super(beginPosition, endPosition);
    }
}
