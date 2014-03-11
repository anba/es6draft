/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.Set;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.6 Iteration Statements</h2>
 * <ul>
 * <li>13.6.4 The for-in and for-of Statements
 * </ul>
 */
public final class ForOfStatement extends IterationStatement implements ScopedNode {
    private final BlockScope scope;
    private final Node head;
    private final Expression expression;
    private final Statement statement;

    public ForOfStatement(long beginPosition, long endPosition, BlockScope scope,
            EnumSet<Abrupt> abrupt, Set<String> labelSet, Node head, Expression expression,
            Statement stmt) {
        super(beginPosition, endPosition, abrupt, labelSet);
        this.scope = scope;
        this.head = head;
        this.expression = expression;
        this.statement = stmt;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    public Node getHead() {
        return head;
    }

    public Expression getExpression() {
        return expression;
    }

    public Statement getStatement() {
        return statement;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
