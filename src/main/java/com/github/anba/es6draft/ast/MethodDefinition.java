/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.3 Method Definitions
 * </ul>
 */
public final class MethodDefinition extends PropertyDefinition implements FunctionNode {
    private final FunctionScope scope;
    private final MethodType type;
    private final boolean isStatic;
    private final PropertyName propertyName;
    private String functionName;
    private final FormalParameterList parameters;
    private List<StatementListItem> statements;
    private StrictMode strictMode;
    private final boolean superReference;
    private final String headerSource, bodySource;
    private boolean syntheticNodes;

    public enum MethodType {
        AsyncFunction, Function, Generator, Getter, Setter
    }

    public MethodDefinition(long beginPosition, long endPosition, FunctionScope scope,
            MethodType type, boolean isStatic, PropertyName propertyName,
            FormalParameterList parameters, List<StatementListItem> statements,
            boolean superReference, String headerSource, String bodySource) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.type = type;
        this.isStatic = isStatic;
        this.propertyName = propertyName;
        this.parameters = parameters;
        this.statements = statements;
        this.superReference = superReference;
        this.headerSource = headerSource;
        this.bodySource = bodySource;
    }

    @Override
    public FunctionScope getScope() {
        return scope;
    }

    public MethodType getType() {
        return type;
    }

    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public PropertyName getPropertyName() {
        return propertyName;
    }

    @Override
    public String getFunctionName() {
        if (functionName != null) {
            return functionName;
        }
        return propertyName.getName();
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
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
    public boolean isGenerator() {
        return getType() == MethodType.Generator;
    }

    @Override
    public boolean isAsync() {
        return getType() == MethodType.AsyncFunction;
    }

    @Override
    public boolean hasSuperReference() {
        return superReference;
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
