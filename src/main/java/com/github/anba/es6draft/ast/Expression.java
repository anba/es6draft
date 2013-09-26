/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1>
 */
public abstract class Expression extends AstNode {
    private int parentheses = 0;

    protected Expression() {
    }

    public boolean isParenthesised() {
        return parentheses != 0;
    }

    public void addParentheses() {
        parentheses += 1;
    }

    public int getParentheses() {
        return parentheses;
    }

    public Expression asValue() {
        return this;
    }
}
