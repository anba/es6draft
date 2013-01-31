/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.Set;

/**
 * <h1>12 Statements and Declarations</h1><br>
 * <h2>12.6 Iteration Statements</h2>
 * <ul>
 * <li>12.6.4 The for-in and for-of Statements
 * </ul>
 */
public class ForOfStatement extends IterationStatement implements ScopedNode {
    private Scope scope;
    private EnumSet<Abrupt> abrupt;
    private Node head;
    private Expression expression;
    private Statement statement;
    private Set<String> labelSet;

    public ForOfStatement(Scope scope, EnumSet<Abrupt> abrupt, Set<String> labelSet, Node head,
            Expression expression, Statement stmt) {
        this.abrupt = abrupt;
        this.labelSet = labelSet;
        this.head = head;
        this.expression = expression;
        this.statement = stmt;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    @Override
    public EnumSet<Abrupt> getAbrupt() {
        return abrupt;
    }

    @Override
    public Set<String> getLabelSet() {
        return labelSet;
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
