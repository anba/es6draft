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
    private final ClassElementName classElementName;
    private final FormalParameterList parameters;
    private List<StatementListItem> statements;
    private final String source;
    private final StrictMode strictMode;
    private String className;
    private String methodName;
    private boolean isSynthetic;

    public enum MethodType {
        AsyncFunction, AsyncGenerator, ClassConstructor, CallConstructor, Function, Generator, Getter, Setter
    }

    public enum MethodAllocation {
        Object, Prototype, Class
    }

    public MethodDefinition(long beginPosition, long endPosition, FunctionScope scope, MethodType type,
            MethodAllocation allocation, List<Expression> decorators, ClassElementName classElementName,
            FormalParameterList parameters, List<StatementListItem> statements, String source, StrictMode strictMode) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.type = type;
        this.allocation = allocation;
        this.decorators = decorators;
        this.classElementName = classElementName;
        this.parameters = parameters;
        this.statements = statements;
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
        return type == MethodType.ClassConstructor;
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
        return classElementName.toPropertyName();
    }

    /**
     * Returns the method's class element name.
     * 
     * @return the method's class element name
     */
    public ClassElementName getClassElementName() {
        return classElementName;
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

    private String propertyName() {
        String pname = getPropertyName().getName();
        if (pname != null) {
            return pname;
        }
        assert classElementName instanceof ComputedPropertyName;
        return ((ComputedPropertyName) classElementName).toString();
    }

    @Override
    public String getMethodName() {
        if (methodName != null) {
            return methodName;
        }

        // Give methods a better name for stacktraces
        if (isClassConstructor()) {
            if (className != null) {
                return className;
            }
            return "constructor";
        }
        if (isCallConstructor()) {
            if (className != null) {
                return className + '.' + "call constructor";
            }
            return "call constructor";
        }
        String name = propertyName();
        if (allocation == MethodAllocation.Prototype && className != null) {
            if (!(classElementName instanceof ComputedPropertyName)) {
                name = className + '.' + name;
            } else {
                name = className + name;
            }
        }
        switch (type) {
        case Getter:
            return "get " + name;
        case Setter:
            return "set " + name;
        default:
            return name;
        }
    }

    @Override
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String getFunctionName() {
        return propertyName();
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

    public boolean isSynthetic() {
        return isSynthetic;
    }

    public void setSynthetic(boolean isSynthetic) {
        this.isSynthetic = isSynthetic;
    }

    @Override
    public String getSource() {
        return source;
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
        case ClassConstructor:
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
