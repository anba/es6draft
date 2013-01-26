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
public class PropertyValueDefinition extends PropertyDefinition {
    private PropertyName propertyName;
    private Expression propertyValue;

    public PropertyValueDefinition(PropertyName propertyName, Expression propertyValue) {
        assert propertyName instanceof Identifier || propertyName instanceof StringLiteral
                || propertyName instanceof NumericLiteral;
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    @Override
    public PropertyName getPropertyName() {
        return propertyName;
    }

    public Expression getPropertyValue() {
        return propertyValue;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
