/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.List;

import com.github.anba.es6draft.ast.scope.ModuleScope;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.internal.Source;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.2 Modules</h2>
 */
public final class Module extends Program implements TopLevelNode<ModuleItem> {
    private final ModuleScope scope;
    private List<ModuleItem> statements;

    public Module(long beginPosition, long endPosition, Source source, ModuleScope scope, List<ModuleItem> statements,
            EnumSet<Parser.Option> parserOptions) {
        super(beginPosition, endPosition, source, parserOptions);
        this.scope = scope;
        this.statements = statements;
    }

    @Override
    public ModuleScope getScope() {
        return scope;
    }

    @Override
    public List<ModuleItem> getStatements() {
        return statements;
    }

    @Override
    public void setStatements(List<ModuleItem> statements) {
        this.statements = statements;
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
