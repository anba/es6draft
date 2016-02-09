/**
 * Copyright (c) 2012-2015 André Bargull
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
    private final MethodAllocation allocation;
    private final List<Expression> decorators;
    private final PropertyName propertyName;
    private final FormalParameterList parameters;
    private List<StatementListItem> statements;
    private final String headerSource, bodySource;
    private String className;
    private StrictMode strictMode;

    public enum MethodType {
        AsyncFunction, AsyncGenerator, BaseConstructor, DerivedConstructor, CallConstructor, Function, Generator,
        ConstructorGenerator, Getter, Setter
    }

    public enum MethodAllocation {
        Object, Prototype, Class
    }

    public MethodDefinition(long beginPosition, long endPosition, FunctionScope scope, MethodType type,
            MethodAllocation allocation, List<Expression> decorators, PropertyName propertyName,
            FormalParameterList parameters, List<StatementListItem> statements, String headerSource,
            String bodySource) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.type = type;
        this.allocation = allocation;
        this.decorators = decorators;
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

    @Override
    public BindingIdentifier getIdentifier() {
        return null;
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
     * Returns the method's allocation kind.
     * 
     * @return the method's allocation kind
     */
    public MethodAllocation getAllocation() {
        return allocation;
    }

    /**
     * Returns {@code true} if this method is a <code>static</code> class method definition.
     * 
     * @return {@code true} if this method is a <code>static</code> method definition
     */
    public boolean isStatic() {
        return allocation == MethodAllocation.Class;
    }

    /**
     * Returns {@code true} if this method is a <code>class constructor</code> method definition.
     * 
     * @return {@code true} if this method is a <code>class constructor</code> method definition
     */
    public boolean isClassConstructor() {
        switch (type) {
        case BaseConstructor:
        case DerivedConstructor:
            return true;
        default:
            return false;
        }
    }

    /**
     * Returns {@code true} if this method is a <code>call constructor</code> method definition.
     * 
     * @return {@code true} if this method is a <code>call constructor</code> method definition
     */
    public boolean isCallConstructor() {
        return type == MethodType.CallConstructor;
    }

    /**
     * Returns the list of method decorators.
     * 
     * @return the list of decorators
     */
    public List<Expression> getDecorators() {
        return decorators;
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
        assert allocation != MethodAllocation.Object;
        this.className = className;
    }

    @Override
    public String getMethodName() {
        // Give methods a better name for stacktraces
        final String fname;
        String pname = propertyName.getName();
        if (pname != null) {
            if (allocation == MethodAllocation.Prototype && className != null) {
                if (isClassConstructor()) {
                    fname = className;
                } else if (isCallConstructor()) {
                    assert "constructor".equals(pname);
                    fname = className + '.' + "call constructor";
                } else {
                    fname = className + '.' + pname;
                }
            } else if (isCallConstructor()) {
                assert "constructor".equals(pname);
                fname = "call constructor";
            } else {
                fname = pname;
            }
        } else {
            assert propertyName instanceof ComputedPropertyName;
            String cname = ((ComputedPropertyName) propertyName).toString();
            if (allocation == MethodAllocation.Prototype && className != null) {
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
        switch (type) {
        case AsyncGenerator:
        case Generator:
        case ConstructorGenerator:
            return true;
        default:
            return false;
        }
    }

    @Override
    public boolean isAsync() {
        switch (type) {
        case AsyncFunction:
        case AsyncGenerator:
            return true;
        default:
            return false;
        }
    }

    @Override
    public boolean isConstructor() {
        switch (type) {
        case BaseConstructor:
        case DerivedConstructor:
        case ConstructorGenerator:
            return true;
        default:
            return false;
        }
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
