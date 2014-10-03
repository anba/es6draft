/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.synthetic.SuperPropertyAccessorValue;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.3 Left-Hand-Side Expressions</h2>
 * <ul>
 * <li>12.3.4 The super Keyword
 * </ul>
 */
public class SuperPropertyAccessor extends LeftHandSideExpression {
    private final String name;

    public SuperPropertyAccessor(long beginPosition, long endPosition, String name) {
        super(beginPosition, endPosition);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Expression asValue() {
        return new SuperPropertyAccessorValue(getBeginPosition(), getEndPosition(), name);
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
