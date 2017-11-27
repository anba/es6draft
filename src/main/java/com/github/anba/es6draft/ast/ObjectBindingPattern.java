/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.3 Declarations and the Variable Statement</h2>
 * <ul>
 * <li>13.3.3 Destructuring Binding Patterns
 * </ul>
 */
public final class ObjectBindingPattern extends BindingPattern {
    private final List<BindingProperty> properties;
    private final BindingRestProperty rest;

    public ObjectBindingPattern(long beginPosition, long endPosition, List<BindingProperty> properties,
            BindingRestProperty rest) {
        super(beginPosition, endPosition);
        this.properties = properties;
        this.rest = rest;
    }

    /**
     * Returns the binding properties of this object binding pattern.
     * 
     * @return the binding properties
     */
    public List<BindingProperty> getProperties() {
        return properties;
    }

    /**
     * Returns the optional binding rest property.
     * 
     * @return the binding rest property or {@code null}
     */
    public BindingRestProperty getRest() {
        return rest;
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
