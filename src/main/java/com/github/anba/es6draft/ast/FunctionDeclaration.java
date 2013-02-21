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
 * <li>13.1 Function Definitions
 * </ul>
 */
public class FunctionDeclaration extends Declaration implements FunctionDefinition {
    private FunctionScope scope;
    private BindingIdentifier identifier;
    private FormalParameterList parameters;
    private List<StatementListItem> statements;
    private boolean strict;
    private String source;

    public FunctionDeclaration(FunctionScope scope, BindingIdentifier identifier,
            FormalParameterList parameters, List<StatementListItem> statements, String source) {
        this.scope = scope;
        this.identifier = identifier;
        this.parameters = parameters;
        this.statements = statements;
        this.source = source;
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
    public FormalParameterList getParameters() {
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

    @Override
    public boolean isConstDeclaration() {
        return false;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
