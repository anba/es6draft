/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

import com.github.anba.es6draft.ast.scope.BlockScope;

/**
 * Extension: Array and Generator Comprehension
 */
public final class LegacyComprehension extends Comprehension implements ScopedNode {
    private final BlockScope scope;

    public LegacyComprehension(BlockScope scope, List<ComprehensionQualifier> list,
            Expression expression) {
        super(list, expression);
        this.scope = scope;
    }

    @Override
    public BlockScope getScope() {
        return scope;
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
