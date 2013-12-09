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
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1>
 * <ul>
 * <li>15.1 Script
 * </ul>
 */
public class Script extends AstNode implements TopLevelNode, ScopedNode {
    private String sourceFile;
    private ScriptScope scope;
    private List<StatementListItem> statements;
    private EnumSet<CompatibilityOption> options;
    private EnumSet<Parser.Option> parserOptions;
    private boolean strict;

    public Script(long beginPosition, long endPosition, String sourceFile, ScriptScope scope,
            List<StatementListItem> statements, EnumSet<CompatibilityOption> options,
            EnumSet<Parser.Option> parserOptions, boolean strict) {
        super(beginPosition, endPosition);
        this.sourceFile = sourceFile;
        this.scope = scope;
        this.statements = statements;
        this.options = options;
        this.parserOptions = parserOptions;
        this.strict = strict;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    @Override
    public ScriptScope getScope() {
        return scope;
    }

    @Override
    public List<StatementListItem> getStatements() {
        return statements;
    }

    public void setStatements(List<StatementListItem> statements) {
        this.statements = statements;
    }

    public boolean isStrict() {
        return strict;
    }

    public EnumSet<CompatibilityOption> getOptions() {
        return options;
    }

    public EnumSet<Parser.Option> getParserOptions() {
        return parserOptions;
    }

    public boolean isEvalScript() {
        return parserOptions.contains(Parser.Option.EvalScript);
    }

    public boolean isDirectEval() {
        return parserOptions.contains(Parser.Option.DirectEval);
    }

    public boolean isGlobalScope() {
        return !parserOptions.contains(Parser.Option.LocalScope);
    }

    public boolean isGlobalCode() {
        return !parserOptions.contains(Parser.Option.FunctionCode);
    }

    public boolean isEnclosedByWithStatement() {
        return parserOptions.contains(Parser.Option.EnclosedByWithStatement);
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
