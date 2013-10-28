/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.List;

import com.github.anba.es6draft.parser.Parser;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1>
 * <ul>
 * <li>15.1 Script
 * </ul>
 */
public class Script extends AstNode implements TopLevelNode, ScopedNode {
    private String sourceFile;
    private FunctionScope scope;
    private List<StatementListItem> statements;
    private EnumSet<Parser.Option> options;
    private boolean strict;

    public Script(long beginPosition, long endPosition, String sourceFile, FunctionScope scope,
            List<StatementListItem> statements, EnumSet<Parser.Option> options, boolean strict) {
        super(beginPosition, endPosition);
        this.sourceFile = sourceFile;
        this.scope = scope;
        this.statements = statements;
        this.options = options;
        this.strict = strict;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    @Override
    public FunctionScope getScope() {
        return scope;
    }

    @Override
    public List<StatementListItem> getStatements() {
        return statements;
    }

    @Override
    public void setStatements(List<StatementListItem> statements) {
        this.statements = statements;
    }

    public boolean isStrict() {
        return strict;
    }

    public EnumSet<Parser.Option> getOptions() {
        return options;
    }

    public boolean isEvalScript() {
        return options.contains(Parser.Option.EvalScript);
    }

    public boolean isDirectEval() {
        return options.contains(Parser.Option.DirectEval);
    }

    public boolean isGlobalScope() {
        return !options.contains(Parser.Option.LocalScope);
    }

    public boolean isGlobalCode() {
        return !options.contains(Parser.Option.FunctionCode);
    }

    public boolean isEnclosedByWithStatement() {
        return options.contains(Parser.Option.EnclosedByWithStatement);
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
