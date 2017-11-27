/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.8 The continue Statement
 * </ul>
 */
public final class ContinueStatement extends Statement {
    private final String label;

    public ContinueStatement(long beginPosition, long endPosition, String label) {
        super(beginPosition, endPosition);
        this.label = label;
    }

    /**
     * Returns the optional {@code continue} label.
     * 
     * @return the label or {@code null}
     */
    public String getLabel() {
        return label;
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
