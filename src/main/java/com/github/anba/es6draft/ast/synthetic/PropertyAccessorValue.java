/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.IntNodeVisitor;
import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.PropertyAccessor;
import com.github.anba.es6draft.ast.VoidNodeVisitor;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.3 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>12.3.1 Property Accessors
 * </ul>
 */
public final class PropertyAccessorValue extends PropertyAccessor {
    public PropertyAccessorValue(long beginPosition, long endPosition, Expression base, String name) {
        super(beginPosition, endPosition, base, name);
    }

    @Override
    public PropertyAccessorValue asValue() {
        return this;
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
