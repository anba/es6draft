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
    private final FormalParameterList parameters;
    private List<StatementListItem> statements;
    private final String headerSource, bodySource;
    private String className;
    private StrictMode strictMode;
    private boolean syntheticNodes;

    public enum MethodType {
        AsyncFunction, Function, Generator, Getter, Setter
    }

    public MethodDefinition(long beginPosition, long endPosition, FunctionScope scope,
            MethodType type, boolean isStatic, PropertyName propertyName,
            FormalParameterList parameters, List<StatementListItem> statements,
            String headerSource, String bodySource) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.type = type;
        this.isStatic = isStatic;
        this.propertyName = propertyName;
        this.parameters = parameters;
        this.statements = statements;
        this.headerSource = headerSource;
        this.bodySource = bodySource;
    }

    @Override
    public FunctionScope getScope() {
        return scope;
    }

    /**
     * Returns the method's type.
     * 
     * @return the method's type
     */
    public MethodType getType() {
        return type;
    }

    /**
     * Returns {@code true} if this method is a <tt>static</tt> class method definition.
     * 
     * @return {@code true} if this method is a <tt>static</tt> method definition
     */
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public PropertyName getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the optional class name.
     * 
     * @return the class name or {@code null} if not available
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set the class name property.
     * 
     * @param className
     *            the class name
     */
    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String getMethodName() {
        // Give methods a better name for stacktraces
        final String fname;
        String pname = propertyName.getName();
        if (pname != null) {
            if (!isStatic && className != null) {
                if ("constructor".equals(pname)) {
                    fname = className;
                } else {
                    fname = className + '.' + pname;
                }
            } else {
                fname = pname;
            }
        } else {
            assert propertyName instanceof ComputedPropertyName;
            String cname = ((ComputedPropertyName) propertyName).toString();
            if (!isStatic && className != null) {
                fname = className + cname;
            } else {
                fname = cname;
            }
        }
        switch (type) {
        case Getter:
            return "get " + fname;
        case Setter:
            return "set " + fname;
        case AsyncFunction:
        case Function:
        case Generator:
        default:
            return fname;
        }
    }

    @Override
    public void setMethodName(String methodName) {
        throw new AssertionError();
    }

    @Override
    public String getFunctionName() {
        final String fname;
        String pname = propertyName.getName();
        if (pname != null) {
            fname = pname;
        } else {
            assert propertyName instanceof ComputedPropertyName;
            fname = ((ComputedPropertyName) propertyName).toString();
        }
        switch (type) {
        case Getter:
            return "get " + fname;
        case Setter:
            return "set " + fname;
        case AsyncFunction:
        case Function:
        case Generator:
        default:
            return fname;
        }
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
        return getType() == MethodType.Generator;
    }

    @Override
    public boolean isAsync() {
        return getType() == MethodType.AsyncFunction;
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
