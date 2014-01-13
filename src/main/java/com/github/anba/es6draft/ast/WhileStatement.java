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
 * <li>13.6.2 The while Statement
 * </ul>
 */
public class WhileStatement extends IterationStatement {
    private EnumSet<Abrupt> abrupt;
    private Set<String> labelSet;
    private Expression test;
    private Statement statement;

    public WhileStatement(long beginPosition, long endPosition, EnumSet<Abrupt> abrupt,
            Set<String> labelSet, Expression test, Statement statement) {
        super(beginPosition, endPosition);
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
