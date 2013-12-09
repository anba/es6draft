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
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.3 Modules</h2>
 */
public class Module extends AstNode implements TopLevelNode, ScopedNode {
    private String sourceFile;
    private ModuleScope scope;
    private List<ModuleItem> statements;
    private EnumSet<CompatibilityOption> options;
    private EnumSet<Parser.Option> parserOptions;

    public Module(long beginPosition, long endPosition, String sourceFile, ModuleScope scope,
            List<ModuleItem> statements, EnumSet<CompatibilityOption> options,
            EnumSet<Parser.Option> parserOptions) {
        super(beginPosition, endPosition);
        this.sourceFile = sourceFile;
        this.scope = scope;
        this.statements = statements;
        this.options = options;
        this.parserOptions = parserOptions;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    @Override
    public ModuleScope getScope() {
        return scope;
    }

    @Override
    public List<ModuleItem> getStatements() {
        return statements;
    }

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
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
