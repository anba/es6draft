/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.13 Assignment Operators</h2>
 * <ul>
 * <li>12.13.1 Destructuring Assignment
 * </ul>
 */
public final class AssignmentProperty extends AstNode {
    private PropertyName propertyName;
    private LeftHandSideExpression target;
    private Expression initialiser;

    public AssignmentProperty(long beginPosition, long endPosition, PropertyName propertyName,
            LeftHandSideExpression target, Expression initialiser) {
        super(beginPosition, endPosition);
        this.propertyName = propertyName;
        this.target = target;
        this.initialiser = initialiser;
    }

    public AssignmentProperty(long beginPosition, long endPosition, Identifier identifier,
            Expression initialiser) {
        super(beginPosition, endPosition);
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
