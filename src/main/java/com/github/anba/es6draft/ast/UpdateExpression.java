/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.4 Update Expressions</h2>
 * <ul>
 * <li>12.4.4 Postfix Increment Operator
 * <li>12.4.5 Postfix Decrement Operator
 * <li>12.4.6 Prefix Increment Operator
 * <li>12.4.7 Prefix Decrement Operator
 * </ul>
 */
public final class UpdateExpression extends Expression {
    public enum Operator {
        PRE_INC("++", false), PRE_DEC("--", false), POST_INC("++", true), POST_DEC("--", true);

        private final String name;
        private final boolean postfix;

        private Operator(String name, boolean postfix) {
            this.name = name;
            this.postfix = postfix;
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

        /**
         * Returns {@code true} if the operator is postfix.
         * 
         * @return {@code true} if postfix operator
         */
        public boolean isPostfix() {
            return postfix;
        }
    }

    private final Operator operator;
    private final Expression operand;
    private boolean completion = true;

    public UpdateExpression(long beginPosition, long endPosition, Operator operator, Expression operand) {
        super(beginPosition, endPosition);
        this.operator = operator;
        this.operand = operand;
    }

    /**
     * Returns the update operator.
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
