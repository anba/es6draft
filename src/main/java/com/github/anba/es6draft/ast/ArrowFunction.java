/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>13 Functions and Generators</h1>
 * <ul>
 * <li>13.2 Arrow Function Definitions
 * </ul>
 */
public class ArrowFunction extends Expression implements FunctionNode {
    private Scope scope;
    private FormalParameterList parameters;
    private List<StatementListItem> statements;
    private Expression expression;
    private boolean strict;
    private String source;

    public ArrowFunction(Scope scope, FormalParameterList parameters,
            List<StatementListItem> statements, String source) {
        this.scope = scope;
        this.parameters = parameters;
        this.statements = statements;
        this.expression = null;
        this.source = source;
    }

    public ArrowFunction(Scope scope, FormalParameterList parameters, Expression expression,
            String source) {
        this.scope = scope;
        this.parameters = parameters;
        this.statements = null;
        this.expression = expression;
        this.source = source;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    @Override
    public FormalParameterList getParameters() {
        return parameters;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public List<StatementListItem> getStatements() {
        return statements;
    }

    @Override
    public boolean isStrict() {
        return strict;
    }

    @Override
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
