/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.12 The switch Statement
 * </ul>
 */
public final class SwitchClause extends AstNode {
    private final Expression expression;
    private List<StatementListItem> statements;

    public SwitchClause(long beginPosition, long endPosition, Expression expression,
            List<StatementListItem> statements) {
        super(beginPosition, endPosition);
        this.expression = expression;
        this.statements = statements;
    }

    /**
     * Returns the <code>CaseClause</code> expression or {@code null} for <code>DefaultClause</code> nodes.
     * 
     * @return the expression or {@code null}
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Returns this node's statement list.
     * 
     * @return the statements
     */
    public List<StatementListItem> getStatements() {
        return statements;
    }

    /**
     * Updates this node's statement list.
     * 
     * @param statements
     *            the new statements
     */
    public void setStatements(List<StatementListItem> statements) {
        assert statements != null;
        this.statements = statements;
    }

    /**
     * Returns {@code true} if this node is a <code>DefaultClause</code>.
     * 
     * @return {@code true} if <code>DefaultClause</code>
     */
    public boolean isDefaultClause() {
        return expression == null;
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
