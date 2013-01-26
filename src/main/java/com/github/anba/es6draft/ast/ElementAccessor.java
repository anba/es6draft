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
public class ElementAccessor extends LeftHandSideExpression {
    private Expression base;
    private Expression element;

    public ElementAccessor(Expression base, Expression element) {
        this.base = base;
        this.element = element;
    }

    public Expression getBase() {
        return base;
    }

    public Expression getElement() {
        return element;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
