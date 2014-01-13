/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.7 The continue Statement
 * </ul>
 */
public class ContinueStatement extends Statement {
    private String label;

    public ContinueStatement(long beginPosition, long endPosition, String label) {
        super(beginPosition, endPosition);
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
