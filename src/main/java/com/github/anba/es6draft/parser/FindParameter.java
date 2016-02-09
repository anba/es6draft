/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.Name;

/**
 * 
 */
final class FindParameter extends DefaultNodeVisitor<BindingIdentifier, Name> {
    private static final FindParameter INSTANCE = new FindParameter();

    /**
     * Returns the named parameter node.
     * 
     * @param name
     *            the parameter name
     * @return the parameter node
     */
    static BindingIdentifier find(FunctionNode function, Name name) {
        BindingIdentifier parameter = function.getParameters().accept(INSTANCE, name);
        assert parameter != null : "Parameter not found: " + name;
        return parameter;
    }

    private BindingIdentifier forEach(Iterable<? extends Node> list, Name name) {
        for (Node node : list) {
            BindingIdentifier result = node.accept(this, name);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    protected BindingIdentifier visit(Node node, Name value) {
        throw new IllegalStateException(node.getClass().toString());
    }

    @Override
    public BindingIdentifier visit(BindingIdentifier node, Name value) {
        return node.getName().equals(value) ? node : null;
    }

    @Override
    public BindingIdentifier visit(FormalParameter node, Name value) {
        return node.getElement().accept(this, value);
    }

    @Override
    public BindingIdentifier visit(FormalParameterList node, Name value) {
        return forEach(node, value);
    }

    @Override
    public BindingIdentifier visit(ArrayBindingPattern node, Name value) {
        return forEach(node.getElements(), value);
    }

    @Override
    public BindingIdentifier visit(BindingElement node, Name value) {
        return node.getBinding().accept(this, value);
    }

    @Override
    public BindingIdentifier visit(BindingRestElement node, Name value) {
        return node.getBindingIdentifier().accept(this, value);
    }

    @Override
    public BindingIdentifier visit(BindingElision node, Name value) {
        return null;
    }

    @Override
    public BindingIdentifier visit(ObjectBindingPattern node, Name value) {
        BindingIdentifier parameter = forEach(node.getProperties(), value);
        if (parameter == null && node.getRest() != null) {
            parameter = node.getRest().accept(this, value);
        }
        return parameter;
    }

    @Override
    public BindingIdentifier visit(BindingProperty node, Name value) {
        return node.getBinding().accept(this, value);
    }

    @Override
    public BindingIdentifier visit(BindingRestProperty node, Name value) {
        return node.getBindingIdentifier().accept(this, value);
    }
}
