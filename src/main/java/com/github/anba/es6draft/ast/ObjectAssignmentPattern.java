/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.13 Assignment Operators</h2>
 * <ul>
 * <li>
 * <li>11.13.1 Destructuring Assignment
 * </ul>
 */
public class ObjectAssignmentPattern extends AssignmentPattern {
    private List<AssignmentProperty> properties;

    public ObjectAssignmentPattern(List<AssignmentProperty> properties) {
        this.properties = properties;
    }

    public List<AssignmentProperty> getProperties() {
        return properties;
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
