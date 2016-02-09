/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

import com.github.anba.es6draft.ast.GeneratorDefinition.GeneratorKind;
import com.github.anba.es6draft.ast.scope.FunctionScope;

/**
 * Extension: Array and Generator Comprehension
 */
public final class GeneratorComprehension extends Expression implements FunctionNode {
    private final FunctionScope scope;
    private final GeneratorKind kind;
    private final FormalParameterList parameters;
    private final Comprehension comprehension;
    private String functionName, methodName;
    private StrictMode strictMode;

    public GeneratorComprehension(long beginPosition, long endPosition, FunctionScope scope, GeneratorKind kind,
            FormalParameterList parameters, Comprehension comprehension) {
        super(beginPosition, endPosition);
        assert parameters.getFormals().isEmpty() : "Non-empty parameter list in comprehension";
        this.scope = scope;
        this.kind = kind;
        this.parameters = parameters;
        this.comprehension = comprehension;
    }

    /**
     * Returns the comprehension part of this generator comprehension.
     * 
     * @return the comprehension part
     */
    public Comprehension getComprehension() {
        return comprehension;
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
        return "gencompr";
    }

    @Override
    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    @Override
    public FormalParameterList getParameters() {
        return parameters;
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
        throw new UnsupportedOperationException();
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
    public boolean isConstructor() {
        return kind == GeneratorKind.Constructor;
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
