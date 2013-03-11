/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.ast.*;

/**
 * 
 */
class IsReference extends DefaultNodeVisitor<Boolean, Void> {
    static final NodeVisitor<Boolean, Void> INSTANCE = new IsReference();

    @Override
    protected Boolean visit(Node node, Void value) {
        // should only be called for "Expression" type nodes
        throw new IllegalStateException();
    }

    @Override
    protected Boolean visit(Expression node, Void value) {
        // all other expressions return a value
        return false;
    }

    @Override
    public Boolean visit(Identifier node, Void value) {
        return true;
    }

    @Override
    public Boolean visit(CallExpression node, Void value) {
        // call expression no longer valid left-hand-side
        return false;
    }

    @Override
    public Boolean visit(ElementAccessor node, Void value) {
        return true;
    }

    @Override
    public Boolean visit(PropertyAccessor node, Void value) {
        return true;
    }

    @Override
    public Boolean visit(SuperExpression node, Void value) {
        return (node.getName() != null || node.getExpression() != null);
    }
}