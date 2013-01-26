/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>12 Statements and Declarations</h1>
 * <ul>
 * <li>12.11 The switch Statement
 * </ul>
 */
public class SwitchClause extends AstNode {
    private Expression expression;
    private List<StatementListItem> statements;

    public SwitchClause(Expression expression, List<StatementListItem> statements) {
        this.expression = expression;
        this.statements = statements;
    }

    public Expression getExpression() {
        return expression;
    }

    public List<StatementListItem> getStatements() {
        return statements;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
