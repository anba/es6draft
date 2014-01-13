/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.semantics;

import com.github.anba.es6draft.ast.DefaultNodeVisitor;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.NodeVisitor;

/**
 * Static Semantics
 */
abstract class StaticSemanticsVisitor<R, V> extends DefaultNodeVisitor<R, V> {
    @Override
    protected final R visit(Node node, V value) {
        throw new IllegalStateException();
    }

    protected static final <V> void forEach(NodeVisitor<?, V> visitor,
            Iterable<? extends Node> list, V value) {
        for (Node node : list) {
            node.accept(visitor, value);
        }
    }

    protected static final <V> boolean every(NodeVisitor<Boolean, V> visitor,
            Iterable<? extends Node> list, V value) {
        for (Node node : list) {
            if (!node.accept(visitor, value)) {
                return false;
            }
        }
        return true;
    }

    protected static final <V> boolean some(NodeVisitor<Boolean, V> visitor,
            Iterable<? extends Node> list, V value) {
        for (Node node : list) {
            if (node.accept(visitor, value)) {
                return true;
            }
        }
        return false;
    }
}
