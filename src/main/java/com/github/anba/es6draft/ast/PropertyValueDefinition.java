/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.1 Primary Expressions</h2>
 * <ul>
 * <li>12.1.5 Object Initialiser
 * </ul>
 */
public final class PropertyValueDefinition extends PropertyDefinition {
    private PropertyName propertyName;
    private Expression propertyValue;

    public PropertyValueDefinition(long beginPosition, long endPosition, PropertyName propertyName,
            Expression propertyValue) {
        super(beginPosition, endPosition);
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
