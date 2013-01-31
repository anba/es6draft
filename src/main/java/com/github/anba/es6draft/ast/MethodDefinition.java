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
 * <li>13.3 Method Definitions
 * </ul>
 */
public class MethodDefinition extends PropertyDefinition implements FunctionNode {
    private Scope scope;
    private MethodType type;
    private PropertyName propertyName;
    private List<FormalParameter> parameters;
    private List<StatementListItem> statements;
    private boolean strict;
    private boolean superReference;
    private String source;

    public enum MethodType {
        Function, Generator, Getter, Setter
    }

    public MethodDefinition(Scope scope, MethodType type, PropertyName propertyName,
            List<FormalParameter> parameters, List<StatementListItem> statements,
            boolean superReference, String source) {
        this.scope = scope;
        this.type = type;
        this.propertyName = propertyName;
        this.parameters = parameters;
        this.statements = statements;
        this.superReference = superReference;
        this.source = source;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    public MethodType getType() {
        return type;
    }

    @Override
    public PropertyName getPropertyName() {
        return propertyName;
    }

    @Override
    public List<FormalParameter> getParameters() {
        return parameters;
    }

    @Override
    public List<StatementListItem> getStatements() {
        return statements;
    }

    @Override
    public boolean isStrict() {
        return strict;
    }

    @Override
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @Override
    public String getSource() {
        return source;
    }

    public boolean hasSuperReference() {
        return superReference;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
