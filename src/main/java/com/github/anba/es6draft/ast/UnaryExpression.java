/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.3 Postfix Expressions</h2>
 * <ul>
 * <li>12.3.1 Postfix Increment Operator
 * <li>12.3.2 Postfix Decrement Operator
 * </ul>
 * <h2>12.4 Unary Operators</h2>
 * <ul>
 * <li>12.4.1 The delete Operator
 * <li>12.4.2 The void Operator
 * <li>12.4.3 The typeof Operator
 * <li>12.4.4 Prefix Increment Operator
 * <li>12.4.5 Prefix Decrement Operator
 * <li>12.4.6 Unary + Operator
 * <li>12.4.7 Unary - Operator
 * <li>12.4.8 Bitwise NOT Operator ( ~ )
 * <li>12.4.9 Logical NOT Operator ( ! )
 * </ul>
 */
public final class UnaryExpression extends Expression {
    public enum Operator {
        DELETE("delete"), VOID("void"), TYPEOF("typeof"), PRE_INC("++"), PRE_DEC("--"), POST_INC(
                "++", true), POST_DEC("--", true), POS("+"), NEG("-"), BITNOT("~"), NOT("!");

        private String name;
        private boolean postfix;

        private Operator(String name) {
            this(name, false);
        }

        private Operator(String name, boolean postfix) {
            this.name = name;
            this.postfix = postfix;
        }

        public String getName() {
            return name;
        }

        public boolean isPostfix() {
            return postfix;
        }
    }

    private Operator operator;
    private Expression operand;

    public UnaryExpression(long beginPosition, long endPosition, Operator operator,
            Expression operand) {
        super(beginPosition, endPosition);
        this.operator = operator;
        this.operand = operand;
    }

    public Operator getOperator() {
        return operator;
    }

    public Expression getOperand() {
        return operand;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
