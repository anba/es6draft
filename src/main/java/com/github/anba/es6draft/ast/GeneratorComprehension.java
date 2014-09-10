/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.Collections;
import java.util.List;

import com.github.anba.es6draft.ast.scope.FunctionScope;

/**
 * Extension: Array and Generator Comprehension
 */
public final class GeneratorComprehension extends Expression implements FunctionNode {
    private final FunctionScope scope;
    private final Comprehension comprehension;
    private String functionName, methodName;
    private StrictMode strictMode;
    private boolean syntheticNodes;

    public GeneratorComprehension(long beginPosition, long endPosition, FunctionScope scope,
            Comprehension comprehension) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.comprehension = comprehension;
    }

    public Comprehension getComprehension() {
        return comprehension;
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
        return "gencompr";
    }

    @Override
    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    @Override
    public FormalParameterList getParameters() {
        return new FormalParameterList(getBeginPosition(), getEndPosition(),
                Collections.<FormalParameter> emptyList());
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
        return "() ";
    }

    @Override
    public String getBodySource() {
        return " [generator comprehension] ";
    }

    @Override
    public List<StatementListItem> getStatements() {
        return null;
    }

    @Override
    public void setStatements(List<StatementListItem> statements) {
        throw new IllegalStateException();
    }

    @Override
    public FunctionScope getScope() {
        return scope;
    }

    @Override
    public ThisMode getThisMode() {
        return ThisMode.Lexical;
    }

    @Override
    public boolean isGenerator() {
        return true;
    }

    @Override
    public boolean isAsync() {
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
