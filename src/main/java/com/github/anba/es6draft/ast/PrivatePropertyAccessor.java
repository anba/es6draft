/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Extension: Private Fields
 */
public final class PrivatePropertyAccessor extends LeftHandSideExpression {
    private final Expression base;
    private final IdentifierName privateName;

    public PrivatePropertyAccessor(long beginPosition, long endPosition, Expression base, IdentifierName privateName) {
        super(beginPosition, endPosition);
        this.base = base;
        this.privateName = privateName;
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
     * Returns the private name.
     * 
     * @return the private name
     */
    public IdentifierName getPrivateName() {
        return privateName;
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
