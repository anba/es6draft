/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import static com.github.anba.es6draft.semantics.StaticSemantics.HasInitialiser;

import com.github.anba.es6draft.ast.ArrayBindingPattern;
import com.github.anba.es6draft.ast.BindingElement;
import com.github.anba.es6draft.ast.BindingIdentifier;
import com.github.anba.es6draft.ast.BindingRestElement;
import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.ObjectBindingPattern;

/**
 * Static Semantics: IsSimpleParameterList
 */
class IsSimpleParameterList extends StaticSemanticsVisitor<Boolean, Void> {
    static final NodeVisitor<Boolean, Void> INSTANCE = new IsSimpleParameterList();

    @Override
    public Boolean visit(BindingRestElement node, Void value) {
        return false;
    }

    @Override
    public Boolean visit(BindingElement node, Void value) {
        if (HasInitialiser(node))
            return false;
        return node.getBinding().accept(this, value);
    }

    @Override
    public Boolean visit(ArrayBindingPattern node, Void value) {
        return false;
    }

    @Override
    public Boolean visit(ObjectBindingPattern node, Void value) {
        return false;
    }

    @Override
    public Boolean visit(BindingIdentifier node, Void value) {
        return true;
    }
}
