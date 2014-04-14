/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.1 Primary Expressions</h2><br>
 * <h3>12.1.4 Array Initializer</h3>
 * <ul>
 * <li>12.1.4.2 Array Comprehension
 * </ul>
 */
public final class ComprehensionFor extends ComprehensionQualifier implements ScopedNode {
    private final BlockScope scope;
    private final Binding binding;
    private final Expression expression;

    public ComprehensionFor(long beginPosition, long endPosition, BlockScope scope,
            Binding binding, Expression expression) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.binding = binding;
        this.expression = expression;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    public Binding getBinding() {
        return binding;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
