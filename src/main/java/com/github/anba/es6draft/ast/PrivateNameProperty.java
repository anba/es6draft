/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.scope.Name;

/**
 * Extension: Class Fields
 */
public final class PrivateNameProperty extends AstNode implements ClassElementName {
    private final IdentifierName identifierName;
    private final Name name;

    public PrivateNameProperty(long beginPosition, long endPosition, IdentifierName identifierName) {
        super(beginPosition, endPosition);
        this.identifierName = identifierName;
        this.name = new Name(identifierName.getName());
    }

    /**
     * Returns the name.
     * 
     * @return the name
     */
    public Name getName() {
        return name;
    }

    @Override
    public IdentifierName toPropertyName() {
        return identifierName;
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
