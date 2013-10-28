/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <ul>
 * <li>Extension: 'let' statement
 * </ul>
 */
public class LetStatement extends Statement implements ScopedNode {
    private BlockScope scope;
    private List<LexicalBinding> bindings;
    private BlockStatement statement;

    public LetStatement(long beginPosition, long endPosition, BlockScope scope,
            List<LexicalBinding> bindings, BlockStatement statement) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.bindings = bindings;
        this.statement = statement;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    public List<LexicalBinding> getBindings() {
        return bindings;
    }

    public BlockStatement getStatement() {
        return statement;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
