/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import com.github.anba.es6draft.ast.ClassFieldDefinition;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.IntNodeVisitor;
import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.ScopedNode;
import com.github.anba.es6draft.ast.Statement;
import com.github.anba.es6draft.ast.VoidNodeVisitor;
import com.github.anba.es6draft.ast.scope.ClassFieldScope;

/**
 * Extension: Class Fields
 */
public final class ClassFieldInitializer extends Statement implements ScopedNode {
    private final ClassFieldDefinition field;
    private final Expression fieldName;

    public ClassFieldInitializer(ClassFieldDefinition field, Expression fieldName) {
        super(field.getBeginPosition(), field.getEndPosition());
        this.field = field;
        this.fieldName = fieldName;
    }

    @Override
    public ClassFieldScope getScope() {
        return field.getScope();
    }

    /**
     * Returns the class field.
     * 
     * @return the class field
     */
    public ClassFieldDefinition getField() {
        return field;
    }

    public Expression getFieldName() {
        return fieldName;
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
