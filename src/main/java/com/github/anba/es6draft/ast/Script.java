/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.List;

import com.github.anba.es6draft.ast.scope.ScriptScope;
import com.github.anba.es6draft.parser.Parser;
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
    private final List<IdentifierName> undeclaredPrivateNames;

    public Script(long beginPosition, long endPosition, Source source, ScriptScope scope,
            List<StatementListItem> statements, EnumSet<Parser.Option> parserOptions, boolean strict,
            List<IdentifierName> undeclaredPrivateNames) {
        super(beginPosition, endPosition, source, parserOptions);
        this.scope = scope;
        this.statements = statements;
        this.strict = strict;
        assert undeclaredPrivateNames.isEmpty() || isEvalScript();
        this.undeclaredPrivateNames = undeclaredPrivateNames;
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

    public List<IdentifierName> getUndeclaredPrivateNames() {
        return undeclaredPrivateNames;
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
