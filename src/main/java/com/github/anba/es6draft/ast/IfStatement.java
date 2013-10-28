/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.5 The if Statement
 * </ul>
 */
public class IfStatement extends Statement {
    private Expression test;
    private Statement then;
    private Statement otherwise;

    public IfStatement(long beginPosition, long endPosition, Expression test, Statement then,
            Statement otherwise) {
        super(beginPosition, endPosition);
        this.test = test;
        this.then = then;
        this.otherwise = otherwise;
    }

    public Expression getTest() {
        return test;
    }

    public Statement getThen() {
        return then;
    }

    public void setThen(Statement then) {
        this.then = then;
    }

    public Statement getOtherwise() {
        return otherwise;
    }

    public void setOtherwise(Statement otherwise) {
        this.otherwise = otherwise;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
