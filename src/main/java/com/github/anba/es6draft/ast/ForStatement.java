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
 * <li>12.6.3 The for Statement
 * </ul>
 */
public class ForStatement extends IterationStatement {
    private EnumSet<Abrupt> abrupt;
    private Set<String> labelSet;
    private Node head;
    private Expression test;
    private Expression step;
    private Statement statement;

    public ForStatement(EnumSet<Abrupt> abrupt, Set<String> labelSet, Node head, Expression test,
            Expression step, Statement statement) {
        this.abrupt = abrupt;
        this.labelSet = labelSet;
        this.head = head;
        this.test = test;
        this.step = step;
        this.statement = statement;
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
