/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2>
 * <ul>
 * <li>12.2.6 Object Initializer
 * </ul>
 */
public final class ObjectLiteral extends Expression {
    private List<PropertyDefinition> properties;
    private final boolean trailingComma;

    public ObjectLiteral(long beginPosition, long endPosition, List<PropertyDefinition> properties,
            boolean trailingComma) {
        super(beginPosition, endPosition);
        this.properties = properties;
        this.trailingComma = trailingComma;
    }

    /**
     * Returns the object property definitions.
     * 
     * @return the object properties
     */
    public List<PropertyDefinition> getProperties() {
        return properties;
    }

    /**
     * Sets the object property definitions.
     * 
     * @param properties
     *            the object properties
     */
    public void setProperties(List<PropertyDefinition> properties) {
        this.properties = properties;
    }

    /**
     * Returns {@code true} if the <code>ObjectLiteral</code> has a trailing comma.
     * 
     * @return {@code true} if the object literal has a trailing comma
     */
    public boolean hasTrailingComma() {
        return trailingComma;
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
