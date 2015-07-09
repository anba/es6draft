/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.6 Multiplicative Operators</h2><br>
 * <h2>12.7 Additive Operators</h2>
 * <ul>
 * <li>12.7.1 The Addition operator ( + )
 * <li>12.7.2 The Subtraction Operator ( - )
 * </ul>
 * <h2>12.8 Bitwise Shift Operators</h2>
 * <ul>
 * <li>12.8.1 The Left Shift Operator ( {@literal <<} )
 * <li>12.8.2 The Signed Right Shift Operator ( {@literal <<} )
 * <li>12.8.3 The Unsigned Right Shift Operator ( {@literal >>>} )
 * </ul>
 * <h2>12.9 Relational Operators</h2><br>
 * <h2>12.10 Equality Operators</h2><br>
 * <h2>12.11 Binary Bitwise Operators</h2><br>
 * <h2>12.12 Binary Logical Operators</h2><br>
 */
public final class BinaryExpression extends Expression {
    public enum Operator {/* @formatter:off */
        EXP("**", 10),
        MUL("*", 9), MOD("%", 9), DIV("/", 9),
        ADD("+", 8), SUB("-", 8),
        SHL("<<", 7), SHR(">>", 7), USHR(">>>", 7),
        LT("<", 6), GT(">", 6), LE("<=", 6), GE(">=", 6), IN("in", 6), INSTANCEOF("instanceof", 6),
        EQ("==", 5), NE("!=", 5), SHEQ("===", 5), SHNE("!==", 5),
        BITAND("&", 4),
        BITXOR("^", 3),
        BITOR("|", 2),
        AND("&&", 1),
        OR("||", 0);
        /* @formatter:on */

        private String name;
        private int precedence;

        private Operator(String name, int precedence) {
            this.name = name;
            this.precedence = precedence;
        }

        public String getName() {
            return name;
        }

        public int getPrecedence() {
            return precedence;
        }

        public boolean isRightAssociative() {
            return this == EXP;
        }
    }

    private final Operator operator;
    private Expression left;
    private Expression right;
    private boolean completion = true;

    public BinaryExpression(Operator operator, Expression left, Expression right) {
        super(left.getBeginPosition(), right.getEndPosition());
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public Operator getOperator() {
        return operator;
    }

    public Expression getLeft() {
        return left;
    }

    public void setLeft(Expression left) {
        this.left = left;
    }

    public Expression getRight() {
        return right;
    }

    public void setRight(Expression right) {
        this.right = right;
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
