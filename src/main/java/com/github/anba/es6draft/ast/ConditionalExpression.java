/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1>
 * <ul>
 * <li>12.13 Conditional Operator ( ? : )
 * </ul>
 */
public final class ConditionalExpression extends Expression {
    private final Expression test;
    private final Expression then;
    private final Expression otherwise;

    public ConditionalExpression(Expression test, Expression then, Expression otherwise) {
        super(test.getBeginPosition(), otherwise.getEndPosition());
        this.test = test;
        this.then = then;
        this.otherwise = otherwise;
    }

    /**
     * Returns the test expression.
     * 
     * @return the test expression
     */
    public Expression getTest() {
        return test;
    }

    /**
     * Returns the consequent expression.
     * 
     * @return the consequent expression
     */
    public Expression getThen() {
        return then;
    }

    /**
     * Returns the alternate expression.
     * 
     * @return the alternate expression
     */
    public Expression getOtherwise() {
        return otherwise;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        visitor.visit(this, value);
    }
}
