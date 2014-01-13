/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.1 Primary Expressions</h2>
 * <ul>
 * <li>12.1.4 Array Initialiser
 * </ul>
 */
public abstract class ArrayInitialiser extends Expression {
    protected ArrayInitialiser(long beginPosition, long endPosition) {
        super(beginPosition, endPosition);
    }
}
