/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import java.util.List;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.IntNodeVisitor;
import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.SpreadElement;
import com.github.anba.es6draft.ast.VoidNodeVisitor;

/**
 * {@link SpreadElement} as an external Java method.
 */
public final class SpreadElementMethod extends Expression {
    private final List<Expression> elements;

    public SpreadElementMethod(List<Expression> elements) {
        super(first(elements).getBeginPosition(), last(elements).getEndPosition());
        this.elements = elements;
    }

    /**
     * Returns the spreaded array elements.
     * 
     * @return the array elements
     */
    public List<Expression> getElements() {
        return elements;
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

    private static Expression first(List<Expression> elements) {
        assert !elements.isEmpty();
        return elements.get(0);
    }

    private static Expression last(List<Expression> elements) {
        assert !elements.isEmpty();
        return elements.get(elements.size() - 1);
    }
}
