/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1>
 * <ul>
 * <li>12.14 Assignment Operators
 * </ul>
 */
public final class AssignmentExpression extends Expression {
    public enum Operator {
        ASSIGN("="), ASSIGN_ADD("+="), ASSIGN_SUB("-="), ASSIGN_MUL("*="), ASSIGN_DIV("/="),
        ASSIGN_MOD("%="), ASSIGN_SHL("<<="), ASSIGN_SHR(">>="), ASSIGN_USHR(">>>="),
        ASSIGN_BITAND("&="), ASSIGN_BITOR("|="), ASSIGN_BITXOR("^="), ASSIGN_EXP("**=");

        private String name;

        private Operator(String name) {
            this.name = name;
        }

        /**
         * Returns the assignment operator name.
         * 
         * @return the operator name
         */
        public String getName() {
            // TODO: This is not a 'name'.
            return name;
        }
    }

    private final Operator operator;
    private final LeftHandSideExpression left;
    private final Expression right;
    private boolean completion = true;

    public AssignmentExpression(Operator operator, LeftHandSideExpression left, Expression right) {
        super(left.getBeginPosition(), right.getEndPosition());
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    /**
     * Returns the assignment operator.
     * 
     * @return the assignment operator
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * Returns the left-hand side assignment target.
     * 
     * @return the assignment target
     */
    public LeftHandSideExpression getLeft() {
        return left;
    }

    /**
     * Returns the right-hand side value expression.
     * 
     * @return the value expression
     */
    public Expression getRight() {
        return right;
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
