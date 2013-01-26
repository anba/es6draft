/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.2 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>11.2.1 Property Accessors
 * </ul>
 */
public class PropertyAccessor extends LeftHandSideExpression {
    private Expression base;
    private String name;

    public PropertyAccessor(Expression base, String name) {
        this.base = base;
        this.name = name;
    }

    public Expression getBase() {
        return base;
    }

    public String getName() {
        return name;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
