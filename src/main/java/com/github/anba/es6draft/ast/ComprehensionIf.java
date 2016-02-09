/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Extension: Array and Generator Comprehension
 */
public final class ComprehensionIf extends ComprehensionQualifier {
    private final Expression test;

    public ComprehensionIf(long beginPosition, long endPosition, Expression test) {
        super(beginPosition, endPosition);
        this.test = test;
    }

    /**
     * Returns the comprehension test expression.
     * 
     * @return the test expression
     */
    public Expression getTest() {
        return test;
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
