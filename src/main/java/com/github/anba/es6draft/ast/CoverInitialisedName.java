/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.1 Primary Expressions</h2>
 * <ul>
 * <li>11.1.5 Object Initialiser
 * </ul>
 */
public class CoverInitialisedName extends PropertyDefinition {
    private Identifier identifier;
    private Expression initialiser;

    public CoverInitialisedName(Identifier identifier, Expression initialiser) {
        this.identifier = identifier;
        this.initialiser = initialiser;
    }

    @Override
    public Identifier getPropertyName() {
        return identifier;
    }

    public Expression getInitialiser() {
        return initialiser;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        throw new IllegalArgumentException();
    }
}
