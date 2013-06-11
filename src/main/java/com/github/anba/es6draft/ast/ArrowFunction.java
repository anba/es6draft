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
    private FunctionScope scope;
    private FormalParameterList parameters;
    private List<StatementListItem> statements;
    private Expression expression;
    private StrictMode strictMode;
    private String headerSource, bodySource;

    public ArrowFunction(FunctionScope scope, FormalParameterList parameters,
            List<StatementListItem> statements, String headerSource, String bodySource) {
        this.scope = scope;
        this.parameters = parameters;
        this.statements = statements;
        this.expression = null;
        this.headerSource = headerSource;
        this.bodySource = bodySource;
    }

    public ArrowFunction(FunctionScope scope, FormalParameterList parameters,
            Expression expression, String headerSource, String bodySource) {
        this.scope = scope;
        this.parameters = parameters;
        this.statements = null;
        this.expression = expression;
        this.headerSource = headerSource;
        this.bodySource = bodySource;
    }

    @Override
    public FunctionScope getScope() {
        return scope;
    }

    @Override
    public String getFunctionName() {
        return "";
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
    public void setStatements(List<StatementListItem> statements) {
        assert expression != null;
        this.statements = statements;
    }

    @Override
    public StrictMode getStrictMode() {
        return strictMode;
    }

    @Override
    public void setStrictMode(StrictMode strictMode) {
        this.strictMode = strictMode;
    }

    @Override
    public String getHeaderSource() {
        return headerSource;
    }

    @Override
    public String getBodySource() {
        return bodySource;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
