/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.List;

import com.github.anba.es6draft.ast.scope.ScriptScope;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Source;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1>
 * <ul>
 * <li>15.1 Script
 * </ul>
 */
public final class Script extends AstNode implements TopLevelNode<StatementListItem>, ScopedNode {
    private final Source source;
    private final ScriptScope scope;
    private List<StatementListItem> statements;
    private final EnumSet<CompatibilityOption> options;
    private final EnumSet<Parser.Option> parserOptions;
    private final boolean strict;
    private boolean syntheticNodes;

    public Script(long beginPosition, long endPosition, Source source, ScriptScope scope,
            List<StatementListItem> statements, EnumSet<CompatibilityOption> options,
            EnumSet<Parser.Option> parserOptions, boolean strict) {
        super(beginPosition, endPosition);
        this.source = source;
        this.scope = scope;
        this.statements = statements;
        this.options = options;
        this.parserOptions = parserOptions;
        this.strict = strict;
    }

    public Source getSource() {
        return source;
    }

    @Override
    public ScriptScope getScope() {
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
