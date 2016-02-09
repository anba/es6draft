/**
 * Copyright (c) 2012-2015 André Bargull
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
 * <li>15.1 Scripts
 * </ul>
 */
public final class Script extends Program implements TopLevelNode<StatementListItem> {
    private final ScriptScope scope;
    private List<StatementListItem> statements;
    private final boolean strict;

    public Script(long beginPosition, long endPosition, Source source, ScriptScope scope,
            List<StatementListItem> statements, EnumSet<CompatibilityOption> options,
            EnumSet<Parser.Option> parserOptions, boolean strict) {
        super(beginPosition, endPosition, source, options, parserOptions);
        this.scope = scope;
        this.statements = statements;
        this.strict = strict;
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

    public boolean isEvalScript() {
        return getParserOptions().contains(Parser.Option.EvalScript);
    }

    public boolean isDirectEval() {
        return getParserOptions().contains(Parser.Option.DirectEval);
    }

    public boolean isGlobalCode() {
        return !getParserOptions().contains(Parser.Option.FunctionCode);
    }

    public boolean isFunctionCode() {
        return getParserOptions().contains(Parser.Option.FunctionCode);
    }

    public boolean isScripting() {
        return getParserOptions().contains(Parser.Option.Scripting);
    }

    public boolean isGlobalScope() {
        return !getParserOptions().contains(Parser.Option.LocalScope);
    }

    public boolean isGlobalThis() {
        return !getParserOptions().contains(Parser.Option.FunctionThis);
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
