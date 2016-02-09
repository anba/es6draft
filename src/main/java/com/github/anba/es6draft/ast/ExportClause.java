/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.2 Modules</h2>
 * <ul>
 * <li>15.2.3 Exports
 * </ul>
 */
public final class ExportClause extends AstNode {
    private final IdentifierName defaultEntry;
    private final List<ExportSpecifier> exports;
    private final IdentifierName nameSpace;

    public ExportClause(long beginPosition, long endPosition, List<ExportSpecifier> exports) {
        super(beginPosition, endPosition);
        this.defaultEntry = null;
        this.exports = exports;
        this.nameSpace = null;
    }

    public ExportClause(long beginPosition, long endPosition, IdentifierName defaultEntry,
            List<ExportSpecifier> namedExports, IdentifierName nameSpace) {
        super(beginPosition, endPosition);
        this.defaultEntry = defaultEntry;
        this.exports = namedExports;
        this.nameSpace = nameSpace;
    }

    public IdentifierName getDefaultEntry() {
        return defaultEntry;
    }

    public List<ExportSpecifier> getExports() {
        return exports;
    }

    public IdentifierName getNameSpace() {
        return nameSpace;
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
