/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.3 Modules</h2>
 */
public final class ExportSpecifier extends AstNode {
    private final String sourceName;
    private final String exportName;

    public ExportSpecifier(long beginPosition, long endPosition, String sourceName,
            String exportName) {
        super(beginPosition, endPosition);
        this.sourceName = sourceName;
        this.exportName = exportName;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getExportName() {
        return exportName;
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
