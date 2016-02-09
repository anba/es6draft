/**
 * Copyright (c) 2012-2016 André Bargull
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
public final class SpreadElementMethod extends SpreadElement implements SyntheticNode {
    private boolean resumePoint;

    public SpreadElementMethod(List<Expression> elements) {
        this(new SpreadArrayLiteral(elements));
    }

    public SpreadElementMethod(SpreadArrayLiteral array) {
        super(array.getBeginPosition(), array.getEndPosition(), array);
    }

    @Override
    public SpreadArrayLiteral getExpression() {
        return (SpreadArrayLiteral) super.getExpression();
    }

    @Override
    public boolean hasResumePoint() {
        return resumePoint;
    }

    @Override
    public void setResumePoint(boolean resumePoint) {
        this.resumePoint = resumePoint;
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
