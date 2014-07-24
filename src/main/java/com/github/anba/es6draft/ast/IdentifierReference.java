/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.synthetic.IdentifierReferenceValue;

/**
 * <h1>12 ECMAScript Language: Expressions</h1>
 * <ul>
 * <li>12.1 Identifiers
 * </ul>
 */
public class IdentifierReference extends LeftHandSideExpression implements PropertyName {
    private final String name;

    public IdentifierReference(long beginPosition, long endPosition, String name) {
        super(beginPosition, endPosition);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IdentifierReferenceValue asValue() {
        return new IdentifierReferenceValue(getBeginPosition(), getEndPosition(), name);
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
