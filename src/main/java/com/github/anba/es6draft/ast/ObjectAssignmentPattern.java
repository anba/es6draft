/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.14 Assignment Operators</h2>
 * <ul>
 * <li>12.14.5 Destructuring Assignment
 * </ul>
 */
public final class ObjectAssignmentPattern extends AssignmentPattern {
    private final List<AssignmentProperty> properties;

    public ObjectAssignmentPattern(long beginPosition, long endPosition,
            List<AssignmentProperty> properties) {
        super(beginPosition, endPosition);
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

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        visitor.visit(this, value);
    }
}
