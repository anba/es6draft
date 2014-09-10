/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * Extension: Array and Generator Comprehension
 */
public class Comprehension extends AstNode {
    private final List<ComprehensionQualifier> list;
    private final Expression expression;

    public Comprehension(List<ComprehensionQualifier> list, Expression expression) {
        super(first(list).getBeginPosition(), expression.getEndPosition());
        this.list = list;
        this.expression = expression;
    }

    public List<ComprehensionQualifier> getList() {
        return list;
    }

    public Expression getExpression() {
        return expression;
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

    private static ComprehensionQualifier first(List<ComprehensionQualifier> list) {
        assert !list.isEmpty();
        return list.get(0);
    }
}
