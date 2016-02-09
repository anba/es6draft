/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.ast.scope.BlockScope;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.2 Block
 * </ul>
 */
public final class BlockStatement extends Statement implements ScopedNode {
    private final BlockScope scope;
    private List<StatementListItem> statements = new ArrayList<>();

    public BlockStatement(long beginPosition, long endPosition, BlockScope scope,
            List<StatementListItem> statements) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.statements = statements;
    }

    @Override
    public BlockScope getScope() {
        return scope;
    }

    /**
     * Returns the list of statement list items.
     * 
     * @return the block statements
     */
    public List<StatementListItem> getStatements() {
        return statements;
    }

    /**
     * Set the list of statement list items.
     * 
     * @param statements
     *            the block statements
     */
    public void setStatements(List<StatementListItem> statements) {
        assert statements != null;
        this.statements = statements;
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
