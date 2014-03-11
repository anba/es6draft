/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.synthetic.PropertyAccessorValue;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>12.2.1 Property Accessors
 * </ul>
 */
public class PropertyAccessor extends LeftHandSideExpression {
    private final Expression base;
    private final String name;

    public PropertyAccessor(long beginPosition, long endPosition, Expression base, String name) {
        super(beginPosition, endPosition);
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
    public PropertyAccessorValue asValue() {
        return new PropertyAccessorValue(getBeginPosition(), getEndPosition(), base, name);
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
