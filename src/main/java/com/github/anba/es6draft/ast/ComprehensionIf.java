/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2><br>
 * <h3>12.2.4 Array Initializer</h3>
 * <ul>
 * <li>12.2.4.2 Array Comprehension
 * </ul>
 */
public final class ComprehensionIf extends ComprehensionQualifier {
    private final Expression test;

    public ComprehensionIf(long beginPosition, long endPosition, Expression test) {
        super(beginPosition, endPosition);
        this.test = test;
    }

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
}
