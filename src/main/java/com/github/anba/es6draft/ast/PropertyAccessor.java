/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.3 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>12.3.2 Property Accessors
 * </ul>
 */
public final class PropertyAccessor extends LeftHandSideExpression {
    private final Expression base;
    private final String name;

    public PropertyAccessor(long beginPosition, long endPosition, Expression base, String name) {
        super(beginPosition, endPosition);
        this.base = base;
        this.name = name;
    }

    /**
     * Returns the base expression.
     * 
     * @return the base expression
     */
    public Expression getBase() {
        return base;
    }

    /**
     * Returns the property name.
     * 
     * @return the property name
     */
    public String getName() {
        return name;
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
