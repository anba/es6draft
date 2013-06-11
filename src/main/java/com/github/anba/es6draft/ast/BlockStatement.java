/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>12 Statements and Declarations</h1>
 * <ul>
 * <li>12.1 Block
 * </ul>
 */
public class BlockStatement extends Statement implements ScopedNode {
    private BlockScope scope;
    private List<StatementListItem> statements = new ArrayList<>();

    public BlockStatement(BlockScope scope, List<StatementListItem> statements) {
        this.scope = scope;
        this.statements = statements;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    public List<StatementListItem> getStatements() {
        return statements;
    }

    public void setStatements(List<StatementListItem> statements) {
        assert statements != null;
        this.statements = statements;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
