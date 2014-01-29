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
 * <li>13.2.1 Let and Const Declarations
 * </ul>
 */
public final class LexicalBinding extends AstNode {
    private Binding binding;
    private Expression initialiser;

    public LexicalBinding(long beginPosition, long endPosition, Binding binding,
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
