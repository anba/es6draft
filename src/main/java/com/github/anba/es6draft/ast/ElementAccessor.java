/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.synthetic.ElementAccessorValue;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.3 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>12.3.1 Property Accessors
 * </ul>
 */
public class ElementAccessor extends LeftHandSideExpression {
    private final Expression base;
    private final Expression element;

    public ElementAccessor(long beginPosition, long endPosition, Expression base, Expression element) {
        super(beginPosition, endPosition);
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
    public ElementAccessorValue asValue() {
        return new ElementAccessorValue(getBeginPosition(), getEndPosition(), base, element);
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
