/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>11 Expressions</h1><br>
 * <h2>11.1 Primary Expressions</h2>
 * <ul>
 * <li>11.1.7 Generator Comprehensions
 * </ul>
 */
public class GeneratorComprehension extends Expression {
    private Expression expression;
    private List<ComprehensionFor> list;
    private Expression test;

    public GeneratorComprehension(Expression expr, List<ComprehensionFor> list, Expression test) {
        this.expression = expr;
        this.list = list;
        this.test = test;
    }

    public Expression getExpression() {
        return expression;
    }

    public List<ComprehensionFor> getList() {
        return list;
    }

    public Expression getTest() {
        return test;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
