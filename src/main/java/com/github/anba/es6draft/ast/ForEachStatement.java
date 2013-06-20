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
 * <li>Extension: 'for-each' statement
 * </ul>
 */
public class ForEachStatement extends IterationStatement implements ScopedNode {
    private BlockScope scope;
    private EnumSet<Abrupt> abrupt;
    private Set<String> labelSet;
    private Node head;
    private Expression expression;
    private Statement statement;

    public ForEachStatement(BlockScope scope, EnumSet<Abrupt> abrupt, Set<String> labelSet,
            Node head, Expression expression, Statement statement) {
        this.scope = scope;
        this.abrupt = abrupt;
        this.labelSet = labelSet;
        this.head = head;
        this.expression = expression;
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
