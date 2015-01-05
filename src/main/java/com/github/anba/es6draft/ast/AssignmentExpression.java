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
 * <li>12.14 Assignment Operators
 * </ul>
 */
public final class AssignmentExpression extends Expression {
    public enum Operator {
        ASSIGN("="), ASSIGN_ADD("+="), ASSIGN_SUB("-="), ASSIGN_MUL("*="), ASSIGN_DIV("/="),
        ASSIGN_MOD("%="), ASSIGN_SHL("<<="), ASSIGN_SHR(">>="), ASSIGN_USHR(">>>="), ASSIGN_BITAND(
                "&="), ASSIGN_BITOR("|="), ASSIGN_BITXOR("^="), ASSIGN_EXP("**=");

        private String name;

        private Operator(String name) {
            this.name = name;
        }

        public String getName() {
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

    public Operator getOperator() {
        return operator;
    }

    public LeftHandSideExpression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    @Override
    public Expression emptyCompletion() {
        completion = false;
        return this;
    }

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
