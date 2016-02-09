/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Extension: Object Rest Destructuring
 */
public final class AssignmentRestProperty extends AstNode {
    private final LeftHandSideExpression target;

    public AssignmentRestProperty(long beginPosition, long endPosition, LeftHandSideExpression target) {
        super(beginPosition, endPosition);
        this.target = target;
    }

    /**
     * Returns the left-hand side expression target.
     * 
     * @return the target expression
     */
    public LeftHandSideExpression getTarget() {
        return target;
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
