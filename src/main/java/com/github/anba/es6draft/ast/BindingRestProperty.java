/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Extension: Object Rest Destructuring
 */
public final class BindingRestProperty extends AstNode {
    private final BindingIdentifier bindingIdentifier;

    public BindingRestProperty(long beginPosition, long endPosition,
            BindingIdentifier bindingIdentifier) {
        super(beginPosition, endPosition);
        this.bindingIdentifier = bindingIdentifier;
    }

    /**
     * Returns the target binding identifier.
     * 
     * @return the binding node
     */
    public BindingIdentifier getBindingIdentifier() {
        return bindingIdentifier;
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
