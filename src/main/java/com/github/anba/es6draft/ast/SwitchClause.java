/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.11 The switch Statement
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

    public Expression getExpression() {
        return expression;
    }

    public List<StatementListItem> getStatements() {
        return statements;
    }

    public void setStatements(List<StatementListItem> statements) {
        assert statements != null;
        this.statements = statements;
    }

    public boolean isDefaultClause() {
        return expression == null;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
