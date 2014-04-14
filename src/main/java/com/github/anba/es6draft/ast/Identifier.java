/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.synthetic.IdentifierValue;

/**
 * <h1>12 ECMAScript Language: Expressions</h1>
 * <ul>
 * <li>12.1 Identifiers
 * </ul>
 */
public class Identifier extends LeftHandSideExpression implements PropertyName {
    private final String name;

    public Identifier(long beginPosition, long endPosition, String name) {
        super(beginPosition, endPosition);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IdentifierValue asValue() {
        return new IdentifierValue(getBeginPosition(), getEndPosition(), name);
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
