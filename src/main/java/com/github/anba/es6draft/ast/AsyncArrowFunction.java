/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * Extension: Async Arrow Function Definition
 */
public final class AsyncArrowFunction extends Expression implements FunctionNode {
    private final FunctionScope scope;
    private final FormalParameterList parameters;
    private List<StatementListItem> statements;
    private final Expression expression;
    private StrictMode strictMode;
    private final String headerSource, bodySource;
    private boolean syntheticNodes;

    public AsyncArrowFunction(long beginPosition, long endPosition, FunctionScope scope,
            FormalParameterList parameters, List<StatementListItem> statements,
            String headerSource, String bodySource) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.parameters = parameters;
        this.statements = statements;
        this.expression = null;
        this.headerSource = headerSource;
        this.bodySource = bodySource;
    }

    public AsyncArrowFunction(long beginPosition, long endPosition, FunctionScope scope,
            FormalParameterList parameters, Expression expression, String headerSource,
            String bodySource) {
        super(beginPosition, endPosition);
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
        assert expression == null;
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
    public ThisMode getThisMode() {
        return ThisMode.Lexical;
    }

    @Override
    public boolean isGenerator() {
        return false;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public boolean hasSuperReference() {
        return false;
    }

    @Override
    public boolean hasSyntheticNodes() {
        return syntheticNodes;
    }

    @Override
    public void setSyntheticNodes(boolean syntheticNodes) {
        this.syntheticNodes = syntheticNodes;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}