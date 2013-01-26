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
 * <li>12.6.1 The do-while Statement
 * </ul>
 */
public class DoWhileStatement extends IterationStatement {
    private EnumSet<Abrupt> abrupt;
    private Set<String> labelSet;
    private Expression test;
    private Statement statement;

    public DoWhileStatement(EnumSet<Abrupt> abrupt, Set<String> labelSet, Expression test,
            Statement statement) {
        this.abrupt = abrupt;
        this.labelSet = labelSet;
        this.test = test;
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

    public Expression getTest() {
        return test;
    }

    public Statement getStatement() {
        return statement;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
