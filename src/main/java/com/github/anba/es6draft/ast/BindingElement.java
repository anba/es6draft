/**
 * Copyright (c) André Bargull
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
public final class BindingElement extends BindingElementItem {
    private final Binding binding;
    private final Expression initializer;

    public BindingElement(long beginPosition, long endPosition, Binding binding, Expression initializer) {
        super(beginPosition, endPosition);
        this.binding = binding;
        this.initializer = initializer;
    }

    /**
     * Returns the target binding.
     * 
     * @return the binding node
     */
    public Binding getBinding() {
        return binding;
    }

    /**
     * Returns the optional initializer expression.
     * 
     * @return the initializer or {@code null}
     */
    public Expression getInitializer() {
        return initializer;
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
