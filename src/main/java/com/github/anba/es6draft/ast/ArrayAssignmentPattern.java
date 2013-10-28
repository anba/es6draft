/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.13 Assignment Operators</h2>
 * <ul>
 * <li>12.13.1 Destructuring Assignment
 * </ul>
 */
public class ArrayAssignmentPattern extends AssignmentPattern {
    private List<AssignmentElementItem> elements;

    public ArrayAssignmentPattern(long beginPosition, long endPosition,
            List<AssignmentElementItem> elements) {
        super(beginPosition, endPosition);
        this.elements = elements;
    }

    public List<AssignmentElementItem> getElements() {
        return elements;
    }

    @Override
    public Expression asValue() {
        throw new IllegalStateException();
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
