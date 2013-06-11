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
 * <li>13.4 Generator Definitions
 * </ul>
 */
public class GeneratorExpression extends Expression implements GeneratorDefinition {
    private FunctionScope scope;
    private BindingIdentifier identifier;
    private String functionName;
    private FormalParameterList parameters;
    private List<StatementListItem> statements;
    private StrictMode strictMode;
    private String headerSource, bodySource;

    public GeneratorExpression(FunctionScope scope, BindingIdentifier identifier,
            FormalParameterList parameters, List<StatementListItem> statements,
            String headerSource, String bodySource) {
        this.scope = scope;
        this.identifier = identifier;
        this.functionName = (identifier != null ? identifier.getName() : "");
        this.parameters = parameters;
        this.statements = statements;
        this.headerSource = headerSource;
        this.bodySource = bodySource;
    }

    public GeneratorExpression(FunctionScope scope, String functionName,
            FormalParameterList parameters, List<StatementListItem> statements,
            String headerSource, String bodySource) {
        this.scope = scope;
        this.identifier = null;
        this.functionName = functionName;
        this.parameters = parameters;
        this.statements = statements;
        this.headerSource = headerSource;
        this.bodySource = bodySource;
    }

    @Override
    public FunctionScope getScope() {
        return scope;
    }

    @Override
    public BindingIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public String getFunctionName() {
        return functionName;
    }

    @Override
    public FormalParameterList getParameters() {
        return parameters;
    }

    @Override
    public List<StatementListItem> getStatements() {
        return statements;
    }

    @Override
    public void setStatements(List<StatementListItem> statements) {
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
