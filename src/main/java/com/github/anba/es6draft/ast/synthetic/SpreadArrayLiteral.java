/**
 * Copyright (c) 2012-2013 André Bargull
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
public class SpreadArrayLiteral extends ArrayLiteral {
    public SpreadArrayLiteral(List<Expression> elements) {
        super(elements);
        assert !elements.isEmpty();
    }

    @Override
    public int getLine() {
        return getElements().get(0).getLine();
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
