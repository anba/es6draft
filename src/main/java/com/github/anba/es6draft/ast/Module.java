/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.EnumSet;
import java.util.List;

import com.github.anba.es6draft.ast.scope.ModuleScope;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Source;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.3 Modules</h2>
 */
public final class Module extends AstNode implements TopLevelNode<ModuleItem>, ScopedNode {
    private final Source source;
    private final ModuleScope scope;
    private List<ModuleItem> statements;
    private final EnumSet<CompatibilityOption> options;
    private final EnumSet<Parser.Option> parserOptions;
    private boolean syntheticNodes;

    public Module(long beginPosition, long endPosition, Source source, ModuleScope scope,
            List<ModuleItem> statements, EnumSet<CompatibilityOption> options,
            EnumSet<Parser.Option> parserOptions) {
        super(beginPosition, endPosition);
        this.source = source;
        this.scope = scope;
        this.statements = statements;
        this.options = options;
        this.parserOptions = parserOptions;
    }

    public Source getSource() {
        return source;
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

    public EnumSet<CompatibilityOption> getOptions() {
        return options;
    }

    public EnumSet<Parser.Option> getParserOptions() {
        return parserOptions;
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
