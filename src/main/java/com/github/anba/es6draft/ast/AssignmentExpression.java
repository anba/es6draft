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
 * <li>12.13 Assignment Operators
 * </ul>
 */
public class AssignmentExpression extends Expression {
    public enum Operator {
        ASSIGN("="), ASSIGN_ADD("+="), ASSIGN_SUB("-="), ASSIGN_MUL("*="), ASSIGN_DIV("/="),
        ASSIGN_MOD("%="), ASSIGN_SHL("<<="), ASSIGN_SHR(">>="), ASSIGN_USHR(">>>="), ASSIGN_BITAND(
                "&="), ASSIGN_BITOR("|="), ASSIGN_BITXOR("^=");

        private String name;

        private Operator(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private Operator operator;
    private LeftHandSideExpression left;
    private Expression right;

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
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
