/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.5 Unary Operators</h2>
 * <ul>
 * <li>12.5.3 The delete Operator
 * <li>12.5.4 The void Operator
 * <li>12.5.5 The typeof Operator
 * <li>12.5.6 Unary + Operator
 * <li>12.5.7 Unary - Operator
 * <li>12.5.8 Bitwise NOT Operator ( ~ )
 * <li>12.5.9 Logical NOT Operator ( ! )
 * </ul>
 */
public final class UnaryExpression extends Expression {
    public enum Operator {
        DELETE("delete"), VOID("void"), TYPEOF("typeof"), POS("+"), NEG("-"), BITNOT("~"), NOT("!");

        private final String name;

        private Operator(String name) {
            this.name = name;
        }

        /**
         * Returns the unary operator expression.
         * 
         * @return the operator expression
         */
        @Override
        public String toString() {
            return name;
        }
    }

    private final Operator operator;
    private final Expression operand;
    private boolean completion = true;

    public UnaryExpression(long beginPosition, long endPosition, Operator operator, Expression operand) {
        super(beginPosition, endPosition);
        this.operator = operator;
        this.operand = operand;
    }

    /**
     * Returns the unary operator.
     * 
     * @return the operator
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * Returns the operand expression.
     * 
     * @return the operand
     */
    public Expression getOperand() {
        return operand;
    }

    @Override
    public Expression emptyCompletion() {
        completion = false;
        return this;
    }

    /**
     * Returns {@code true} if the completion value is used.
     * 
     * @return {@code true} if the completion value is used
     * @see #emptyCompletion()
     */
    public boolean hasCompletion() {
        return completion;
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
