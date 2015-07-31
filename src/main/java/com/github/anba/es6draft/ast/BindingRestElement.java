/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.3 Declarations and the Variable Statement</h2>
 * <ul>
 * <li>13.3.3 Destructuring Binding Patterns
 * </ul>
 */
public final class BindingRestElement extends BindingElementItem {
    private final BindingIdentifier bindingIdentifier;

    public BindingRestElement(long beginPosition, long endPosition,
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
