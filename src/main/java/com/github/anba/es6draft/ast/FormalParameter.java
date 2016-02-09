/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.scope.Scope;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.1 Function Definitions
 * </ul>
 */
public final class FormalParameter extends AstNode implements ScopedNode {
    private final BindingElementItem element;
    private final Scope scope;

    public FormalParameter(long beginPosition, long endPosition, BindingElement element, Scope scope) {
        super(beginPosition, endPosition);
        this.element = element;
        this.scope = scope;
    }

    public FormalParameter(long beginPosition, long endPosition, BindingRestElement element) {
        super(beginPosition, endPosition);
        this.element = element;
        this.scope = null;
    }

    /**
     * Returns the binding element.
     * 
     * @return the binding element
     */
    public BindingElementItem getElement() {
        return element;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code null} for simple or rest formal parameters.
     */
    @Override
    public Scope getScope() {
        return scope;
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
