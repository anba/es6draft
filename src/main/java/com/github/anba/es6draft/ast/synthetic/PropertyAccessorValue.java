/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.PropertyAccessor;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>12.2.1 Property Accessors
 * </ul>
 */
public class PropertyAccessorValue extends PropertyAccessor {
    public PropertyAccessorValue(long sourcePosition, Expression base, String name) {
        super(sourcePosition, base, name);
    }

    @Override
    public PropertyAccessorValue asValue() {
        return this;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
