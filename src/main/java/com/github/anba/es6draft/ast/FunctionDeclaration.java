/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

import com.github.anba.es6draft.ast.scope.FunctionScope;
import com.github.anba.es6draft.ast.scope.Name;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.1 Function Definitions
 * </ul>
 */
public final class FunctionDeclaration extends HoistableDeclaration implements FunctionDefinition {
    private final FunctionScope scope;
    private final BindingIdentifier identifier;
    private final Name name;
    private final FormalParameterList parameters;
    private List<StatementListItem> statements;
    private final String functionName;
    private final String source;
    private final StrictMode strictMode;
    private boolean legacyBlockScoped;
    private int legacyBlockScopeId;

    public FunctionDeclaration(long beginPosition, long endPosition, FunctionScope scope, BindingIdentifier identifier,
            FormalParameterList parameters, List<StatementListItem> statements, String functionName, String source,
            StrictMode strictMode) {
        super(beginPosition, endPosition);
        this.scope = scope;
        this.identifier = identifier;
        this.name = identifier != null ? identifier.getName() : new Name(Name.DEFAULT_EXPORT);
        this.parameters = parameters;
        this.statements = statements;
        this.functionName = functionName;
        this.source = source;
        this.strictMode = strictMode;
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
    public Name getName() {
        return name;
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
        return functionName;
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
    public String getSource() {
        return source;
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
        return false;
    }

    @Override
    public boolean isConstructor() {
        return true;
    }

    @Override
    public boolean isConstDeclaration() {
        return false;
    }

    /**
     * Returns {@code true} if this function is legacy block-level scoped function declaration.
     * 
     * @return {@code true} if legacy block-level scoped function declaration
     */
    public boolean isLegacyBlockScoped() {
        return legacyBlockScoped;
    }

    /**
     * Enables or disables legacy block-level scoped function declaration behavior for this function.
     * 
     * @param legacyBlockScoped
     *            {@code true} for legacy block scoped functions
     */
    public void setLegacyBlockScoped(boolean legacyBlockScoped) {
        this.legacyBlockScoped = legacyBlockScoped;
    }

    /**
     * Returns the function identifier for a legacy block-level scoped function declaration.
     * 
     * @return the legacy block scope identifier
     */
    public int getLegacyBlockScopeId() {
        return legacyBlockScopeId;
    }

    /**
     * Sets the function identifier for a legacy block-level scoped function declaration.
     * 
     * @param legacyBlockScopeId
     *            the legacy block scope identifier (positive integer)
     */
    public void setLegacyBlockScopeId(int legacyBlockScopeId) {
        assert legacyBlockScopeId > 0;
        this.legacyBlockScopeId = legacyBlockScopeId;
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
