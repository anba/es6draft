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
public class ForStatement extends IterationStatement implements ScopedNode {
    private BlockScope scope;
    private EnumSet<Abrupt> abrupt;
    private Set<String> labelSet;
    private Node head;
    private Expression test;
    private Expression step;
    private Statement statement;

    public ForStatement(long beginPosition, long endPosition, BlockScope scope,
            EnumSet<Abrupt> abrupt, Set<String> labelSet, Node head, Expression test,
            Expression step, Statement statement) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.abrupt = abrupt;
        this.labelSet = labelSet;
        this.head = head;
        this.test = test;
        this.step = step;
        this.statement = statement;
    }

    @Override
    public BlockScope getScope() {
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
