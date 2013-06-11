/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.synthetic;

import com.github.anba.es6draft.ast.NodeVisitor;
import com.github.anba.es6draft.ast.SpreadElement;

/**
 * {@link SpreadElement} as an external Java method
 */
public class SpreadElementMethod extends SpreadElement {
    public SpreadElementMethod(SpreadArrayLiteral array) {
        super(array);
    }

    @Override
    public int getLine() {
        return getExpression().getLine();
    }

    @Override
    public SpreadArrayLiteral getExpression() {
        return (SpreadArrayLiteral) super.getExpression();
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
