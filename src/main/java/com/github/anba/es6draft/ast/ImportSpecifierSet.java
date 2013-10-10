/**
 * Copyright (c) 2012-2013 André Bargull
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
public class ImportSpecifierSet extends AstNode {
    private String defaultImport;
    private List<ImportSpecifier> imports;

    public ImportSpecifierSet(long sourcePosition, String defaultImport) {
        super(sourcePosition);
        this.defaultImport = defaultImport;
    }

    public ImportSpecifierSet(long sourcePosition, List<ImportSpecifier> imports) {
        super(sourcePosition);
        this.imports = imports;
    }

    public String getDefaultImport() {
        return defaultImport;
    }

    public List<ImportSpecifier> getImports() {
        return imports;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
