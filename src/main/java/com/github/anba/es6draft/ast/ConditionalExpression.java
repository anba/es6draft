/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1>
 * <ul>
 * <li>12.12 Conditional Operator ( ? : )
 * </ul>
 */
public class ConditionalExpression extends Expression {
    private Expression test;
    private Expression then;
    private Expression otherwise;

    public ConditionalExpression(Expression test, Expression then, Expression otherwise) {
        super(test.getBeginPosition(), otherwise.getEndPosition());
        this.test = test;
        this.then = then;
        this.otherwise = otherwise;
    }

    public Expression getTest() {
        return test;
    }

    public Expression getThen() {
        return then;
    }

    public Expression getOtherwise() {
        return otherwise;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
