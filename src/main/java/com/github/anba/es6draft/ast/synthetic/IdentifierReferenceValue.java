/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import com.github.anba.es6draft.ast.IdentifierReference;
import com.github.anba.es6draft.ast.IntNodeVisitor;
import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.VoidNodeVisitor;

/**
 * <h1>12 ECMAScript Language: Expressions</h1>
 * <ul>
 * <li>12.1 Identifiers
 * </ul>
 */
public final class IdentifierReferenceValue extends IdentifierReference {
    public IdentifierReferenceValue(long beginPosition, long endPosition, String name) {
        super(beginPosition, endPosition, name);
    }

    @Override
    public IdentifierReferenceValue asValue() {
        return this;
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
