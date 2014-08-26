/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

import com.github.anba.es6draft.ast.scope.FunctionScope;

/**
 * Extension: Async Function Declaration
 */
public final class AsyncFunctionDeclaration extends Declaration implements AsyncFunctionDefinition {
    private final FunctionScope scope;
    private final BindingIdentifier identifier;
    private final FormalParameterList parameters;
    private List<StatementListItem> statements;
    private final String headerSource, bodySource;
    private StrictMode strictMode;
    private boolean syntheticNodes;

    public AsyncFunctionDeclaration(long beginPosition, long endPosition, FunctionScope scope,
            BindingIdentifier identifier, FormalParameterList parameters,
            List<StatementListItem> statements, String headerSource, String bodySource) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.identifier = identifier;
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
    public String getMethodName() {
        return getFunctionName();
    }

    @Override
    public void setMethodName(String methodName) {
        throw new AssertionError();
    }

    @Override
    public String getFunctionName() {
        return identifier.getName().getIdentifier();
    }

    @Override
    public void setFunctionName(String functionName) {
        throw new AssertionError();
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
    public ThisMode getThisMode() {
        return strictMode == StrictMode.NonStrict ? ThisMode.Global : ThisMode.Strict;
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
    public boolean isConstDeclaration() {
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

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        visitor.visit(this, value);
    }
}
