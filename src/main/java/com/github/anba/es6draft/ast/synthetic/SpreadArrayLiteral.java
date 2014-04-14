/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import java.util.List;

import com.github.anba.es6draft.ast.ArrayLiteral;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.NodeVisitor;

/**
 * {@link ArrayLiteral} subclass for {@link SpreadElementMethod}
 */
public final class SpreadArrayLiteral extends ArrayLiteral {
    public SpreadArrayLiteral(List<Expression> elements) {
        super(first(elements).getBeginPosition(), last(elements).getEndPosition(), elements, false);
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
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
