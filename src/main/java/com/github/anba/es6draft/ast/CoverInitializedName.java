/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2>
 * <ul>
 * <li>12.2.5 Object Initializer
 * </ul>
 */
public final class CoverInitializedName extends PropertyDefinition {
    private final IdentifierReference identifier;
    private final Expression initializer;

    public CoverInitializedName(long beginPosition, long endPosition,
            IdentifierReference identifier, Expression initializer) {
        super(beginPosition, endPosition);
        this.identifier = identifier;
        this.initializer = initializer;
    }

    @Override
    public IdentifierReference getPropertyName() {
        return identifier;
    }

    public Expression getInitializer() {
        return initializer;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        throw new AssertionError();
    }

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        throw new AssertionError();
    }

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        throw new AssertionError();
    }
}
