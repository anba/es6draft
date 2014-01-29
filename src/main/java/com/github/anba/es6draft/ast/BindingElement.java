/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.2 Declarations and the Variable Statement</h2>
 * <ul>
 * <li>13.2.4 Destructuring Binding Patterns
 * </ul>
 */
public final class BindingElement extends FormalParameter implements BindingElementItem {
    private Binding binding;
    private Expression initialiser;

    public BindingElement(long beginPosition, long endPosition, Binding binding,
            Expression initialiser) {
        super(beginPosition, endPosition);
        this.binding = binding;
        this.initialiser = initialiser;
    }

    public Binding getBinding() {
        return binding;
    }

    public Expression getInitialiser() {
        return initialiser;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
