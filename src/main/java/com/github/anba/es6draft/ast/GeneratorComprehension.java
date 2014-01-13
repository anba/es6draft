/**
 * Copyright (c) 2012-2014 André Bargull
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
public class GeneratorComprehension extends Expression implements MethodNode {
    private Comprehension comprehension;
    private boolean syntheticNodes;

    public GeneratorComprehension(long beginPosition, long endPosition, Comprehension comprehension) {
        super(beginPosition, endPosition);
        this.comprehension = comprehension;
    }

    public Comprehension getComprehension() {
        return comprehension;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public boolean hasSyntheticNodes() {
        return syntheticNodes;
    }

    @Override
    public void setSyntheticNodes(boolean syntheticNodes) {
        this.syntheticNodes = syntheticNodes;
    }
}
