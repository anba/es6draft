/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.3 Modules</h2>
 */
public class ImportDeclaration extends StatementListItem {
    private ImportSpecifierSet importSpecifierSet;
    private String moduleSpecifier;

    public ImportDeclaration(long sourcePosition, String moduleSpecifier) {
        super(sourcePosition);
        this.moduleSpecifier = moduleSpecifier;
    }

    public ImportDeclaration(long sourcePosition, ImportSpecifierSet importSpecifierSet,
            String moduleSpecifier) {
        super(sourcePosition);
        this.importSpecifierSet = importSpecifierSet;
        this.moduleSpecifier = moduleSpecifier;
    }

    public ImportSpecifierSet getImportSpecifierSet() {
        return importSpecifierSet;
    }

    public String getModuleSpecifier() {
        return moduleSpecifier;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
