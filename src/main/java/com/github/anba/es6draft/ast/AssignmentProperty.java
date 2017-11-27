/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.14 Assignment Operators</h2>
 * <ul>
 * <li>12.14.1 Destructuring Assignment
 * </ul>
 */
public final class AssignmentProperty extends AstNode {
    private final PropertyName propertyName;
    private final LeftHandSideExpression target;
    private final Expression initializer;

    public AssignmentProperty(long beginPosition, long endPosition, PropertyName propertyName,
            LeftHandSideExpression target, Expression initializer) {
        super(beginPosition, endPosition);
        this.propertyName = propertyName;
        this.target = target;
        this.initializer = initializer;
    }

    public AssignmentProperty(long beginPosition, long endPosition, IdentifierReference identifier,
            Expression initializer) {
        super(beginPosition, endPosition);
        this.propertyName = null;
        this.target = identifier;
        this.initializer = initializer;
    }

    /**
     * Returns the optional property name.
     * 
     * @return the property name or {@code null}
     */
    public PropertyName getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the left-hand side expression target.
     * 
     * @return the target expression
     */
    public LeftHandSideExpression getTarget() {
        return target;
    }

    /**
     * Returns the optional initializer expression.
     * 
     * @return the initializer or {@code null}
     */
    public Expression getInitializer() {
        return initializer;
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
