/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.1 Primary Expressions</h2>
 * <ul>
 * <li>12.1.7 Generator Comprehensions
 * </ul>
 */
public class GeneratorComprehension extends Expression {
    private Comprehension comprehension;

    public GeneratorComprehension(long sourcePosition, Comprehension comprehension) {
        super(sourcePosition);
        this.comprehension = comprehension;
    }

    public Comprehension getComprehension() {
        return comprehension;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
