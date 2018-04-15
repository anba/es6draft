/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

import com.github.anba.es6draft.ast.scope.FunctionScope;

/**
 * Extension: Async Arrow Function Definition
 */
public final class AsyncArrowFunction extends Expression implements FunctionNode {
    private final FunctionScope scope;
    private final FormalParameterList parameters;
    private List<StatementListItem> statements;
    private final Expression expression;
    private final String source;
    private final StrictMode strictMode;
    private String functionName, methodName;

    public AsyncArrowFunction(long beginPosition, long endPosition, FunctionScope scope, FormalParameterList parameters,
            List<StatementListItem> statements, String source, StrictMode strictMode) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.parameters = parameters;
        this.statements = statements;
        this.expression = null;
        this.source = source;
        this.strictMode = strictMode;
    }

    public AsyncArrowFunction(long beginPosition, long endPosition, FunctionScope scope, FormalParameterList parameters,
            Expression expression, String source, StrictMode strictMode) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.parameters = parameters;
        this.statements = null;
        this.expression = expression;
        this.source = source;
        this.strictMode = strictMode;
    }

    @Override
    public FunctionScope getScope() {
        return scope;
    }

    @Override
    public BindingIdentifier getIdentifier() {
        return null;
    }

    @Override
    public String getMethodName() {
        if (methodName != null) {
            return methodName;
        }
        return getFunctionName();
    }

    @Override
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String getFunctionName() {
        if (functionName != null) {
            return functionName;
        }
        return "";
    }

    @Override
    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    @Override
    public FormalParameterList getParameters() {
        return parameters;
    }

    /**
     * Returns the expression for a concise async arrow function.
     * 
     * @return the expression or {@code null}
     */
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
    public String getSource() {
        return source;
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
    public boolean isConstructor() {
        return false;
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
