/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.13 Assignment Operators</h2>
 * <ul>
 * <li>
 * <li>11.13.1 Destructuring Assignment
 * </ul>
 */
public class AssignmentProperty extends AstNode {
    private PropertyName propertyName;
    private LeftHandSideExpression target;
    private Expression initialiser;

    public AssignmentProperty(PropertyName propertyName, LeftHandSideExpression target,
            Expression initialiser) {
        this.propertyName = propertyName;
        this.target = target;
        this.initialiser = initialiser;
    }

    public AssignmentProperty(Identifier identifier, Expression initialiser) {
        this.propertyName = null;
        this.target = identifier;
        this.initialiser = initialiser;
    }

    public PropertyName getPropertyName() {
        return propertyName;
    }

    public LeftHandSideExpression getTarget() {
        return target;
    }

    public Expression getInitialiser() {
        return initialiser;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
