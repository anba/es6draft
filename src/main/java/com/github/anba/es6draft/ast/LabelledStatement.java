/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.Set;

import com.github.anba.es6draft.ast.BreakableStatement.Abrupt;

/**
 * <h1>12 Statements and Declarations</h1>
 * <ul>
 * <li>12.12 Labelled Statements
 * </ul>
 */
public class LabelledStatement extends Statement {
    private EnumSet<Abrupt> abrupt;
    private Set<String> labels;
    private Statement statement;

    public LabelledStatement(EnumSet<Abrupt> abrupt, Set<String> labels, Statement statement) {
        this.abrupt = abrupt;
        this.labels = labels;
        this.statement = statement;
    }

    public EnumSet<Abrupt> getAbrupt() {
        return abrupt;
    }

    public Set<String> getLabels() {
        return labels;
    }

    public Statement getStatement() {
        return statement;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
