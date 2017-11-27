/**
 * Copyright (c) André Bargull
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
 * <li>13.13 Labelled Statements
 * </ul>
 */
public final class LabelledStatement extends Statement implements AbruptNode {
    private final EnumSet<Abrupt> abrupt;
    private final Set<String> labels;
    private final Statement statement;

    public LabelledStatement(long beginPosition, long endPosition, EnumSet<Abrupt> abrupt, Set<String> labels,
            Statement statement) {
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

    /**
     * Returns <code>true</code> if this node is the target of a <code>BreakStatement</code>.
     * 
     * @return <code>true</code> if this node is the target of a <code>BreakStatement</code>
     */
    public boolean hasBreak() {
        return getAbrupt().contains(Abrupt.Break);
    }

    /**
     * Returns the inner statement enclosed by this labelled statement.
     * 
     * @return the statement
     */
    public Statement getStatement() {
        return statement;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        visitor.visit(this, value);
    }
}
