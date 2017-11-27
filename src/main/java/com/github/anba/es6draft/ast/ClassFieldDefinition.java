/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.scope.ClassFieldScope;

/**
 * Extension: Class Fields
 */
public final class ClassFieldDefinition extends PropertyDefinition {
    private final FieldAllocation allocation;
    private final ClassFieldScope scope;
    private final ClassElementName classElementName;
    private final Expression initializer;

    public enum FieldAllocation {
        Prototype, Class
    }

    public ClassFieldDefinition(long beginPosition, long endPosition, FieldAllocation allocation, ClassFieldScope scope,
            ClassElementName classElementName, Expression initializer) {
        super(beginPosition, endPosition);
        this.allocation = allocation;
        this.scope = scope;
        this.classElementName = classElementName;
        this.initializer = initializer;
    }

    @Override
    public PropertyName getPropertyName() {
        return classElementName.toPropertyName();
    }

    /**
     * Returns the field's allocation kind.
     * 
     * @return the field's allocation kind
     */
    public FieldAllocation getAllocation() {
        return allocation;
    }

    /**
     * Returns {@code true} if this field is a <code>static</code> class field definition.
     * 
     * @return {@code true} if this field is a <code>static</code> field definition
     */
    public boolean isStatic() {
        return allocation == FieldAllocation.Class;
    }

    /**
     * Returns the field's scope.
     * 
     * @return the class field initializer scope
     */
    public ClassFieldScope getScope() {
        return scope;
    }

    /**
     * Returns the field's class element name.
     * 
     * @return the field's class element name
     */
    public ClassElementName getClassElementName() {
        return classElementName;
    }

    /**
     * Returns the initializer expression.
     * 
     * @return the initializer expression or {@code null} if not present
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
