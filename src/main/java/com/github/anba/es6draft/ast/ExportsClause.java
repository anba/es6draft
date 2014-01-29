/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.3 Modules</h2>
 */
public final class ExportsClause extends AstNode {
    private List<ExportSpecifier> exports;

    public ExportsClause(long beginPosition, long endPosition, List<ExportSpecifier> exports) {
        super(beginPosition, endPosition);
        this.exports = exports;
    }

    public List<ExportSpecifier> getExports() {
        return exports;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
