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
 * <li>13.6.3 The for Statement
 * </ul>
 */
public final class ForStatement extends IterationStatement implements ScopedNode {
    private final BlockScope scope;
    private final Node head;
    private final Expression test;
    private final Expression step;
    private final Statement statement;

    public ForStatement(long beginPosition, long endPosition, BlockScope scope,
            EnumSet<Abrupt> abrupt, Set<String> labelSet, Node head, Expression test,
            Expression step, Statement statement) {
        super(beginPosition, endPosition, abrupt, labelSet);
        this.scope = scope;
        this.head = head;
        this.test = test;
        this.step = step;
        this.statement = statement;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    public Node getHead() {
        return head;
    }

    public Expression getTest() {
        return test;
    }

    public Expression getStep() {
        return step;
    }

    public Statement getStatement() {
        return statement;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
