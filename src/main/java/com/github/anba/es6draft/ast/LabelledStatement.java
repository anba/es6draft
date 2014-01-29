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
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.12 Labelled Statements
 * </ul>
 */
public final class LabelledStatement extends Statement implements AbruptNode {
    private EnumSet<Abrupt> abrupt;
    private Set<String> labels;
    private Statement statement;

    public LabelledStatement(long beginPosition, long endPosition, EnumSet<Abrupt> abrupt,
            Set<String> labels, Statement statement) {
        super(beginPosition, endPosition);
        this.abrupt = abrupt;
        this.labels = labels;
        this.statement = statement;
    }

    @Override
    public EnumSet<Abrupt> getAbrupt() {
        return abrupt;
    }

    @Override
    public Set<String> getLabelSet() {
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
