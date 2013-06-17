/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import com.github.anba.es6draft.ast.ElementAccessor;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.NodeVisitor;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.2 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>11.2.1 Property Accessors
 * </ul>
 */
public class ElementAccessorValue extends ElementAccessor {
    public ElementAccessorValue(Expression base, Expression element) {
        super(base, element);
    }

    @Override
    public ElementAccessorValue asValue() {
        return this;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
