/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>14 Scripts and Modules</h1>
 * <ul>
 * <li>14.1 Script
 * </ul>
 */
public class Script extends AstNode {
    private String sourceFile;
    private List<StatementListItem> statements;
    private boolean strict;
    private boolean global;

    public Script(String sourceFile, List<StatementListItem> statements, boolean strict,
            boolean global) {
        this.sourceFile = sourceFile;
        this.statements = statements;
        this.strict = strict;
        this.global = global;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public List<StatementListItem> getStatements() {
        return statements;
    }

    public boolean isStrict() {
        return strict;
    }

    public boolean isGlobal() {
        return global;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
