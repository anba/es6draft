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
public final class ImportClause extends AstNode {
    private final BindingIdentifier defaultEntry;
    private final List<ImportSpecifier> namedImports;

    public ImportClause(long beginPosition, long endPosition, BindingIdentifier defaultEntry,
            List<ImportSpecifier> namedImports) {
        super(beginPosition, endPosition);
        this.defaultEntry = defaultEntry;
        this.namedImports = namedImports;
    }

    public BindingIdentifier getDefaultEntry() {
        return defaultEntry;
    }

    public List<ImportSpecifier> getNamedImports() {
        return namedImports;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }

    @Override
    public <V> int accept(IntNodeVisitor<V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
