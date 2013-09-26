/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.1 Primary Expressions</h2>
 * <ul>
 * <li>Extension: 'let' expression
 * </ul>
 */
public class LetExpression extends Expression implements ScopedNode {
    private BlockScope scope;
    private List<LexicalBinding> bindings;
    private Expression expression;

    public LetExpression(BlockScope scope, List<LexicalBinding> bindings, Expression expression) {
        this.scope = scope;
        this.bindings = bindings;
        this.expression = expression;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    public List<LexicalBinding> getBindings() {
        return bindings;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
