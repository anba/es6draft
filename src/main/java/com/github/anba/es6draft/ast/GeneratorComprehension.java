/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.Collections;
import java.util.List;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.1 Primary Expressions</h2>
 * <ul>
 * <li>12.1.7 Generator Comprehensions
 * </ul>
 */
public final class GeneratorComprehension extends Expression implements FunctionNode {
    private FunctionScope scope;
    private Comprehension comprehension;
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
    public String getFunctionName() {
        return "gencompr";
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
        return "function gencompr() ";
    }

    @Override
    public String getBodySource() {
        return "\n  /* generator comprehension */\n";
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
    public boolean isGenerator() {
        return true;
    }

    @Override
    public boolean hasSuperReference() {
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
}
