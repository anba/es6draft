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
public final class ImportDeclaration extends ModuleItem {
    private final Type type;
    private final ModuleImport moduleImport;
    private final ImportClause importClause;
    private final String moduleSpecifier;

    public enum Type {
        ModuleImport, ImportModule, ImportFrom
    }

    public ImportDeclaration(long beginPosition, long endPosition, ModuleImport moduleImport) {
        super(beginPosition, endPosition);
        this.type = Type.ModuleImport;
        this.moduleImport = moduleImport;
        this.importClause = null;
        this.moduleSpecifier = null;
    }

    public ImportDeclaration(long beginPosition, long endPosition, ImportClause importClause,
            String moduleSpecifier) {
        super(beginPosition, endPosition);
        this.type = Type.ImportFrom;
        this.importClause = importClause;
        this.moduleSpecifier = moduleSpecifier;
        this.moduleImport = null;
    }

    public ImportDeclaration(long beginPosition, long endPosition, String moduleSpecifier) {
        super(beginPosition, endPosition);
        this.type = Type.ImportModule;
        this.moduleSpecifier = moduleSpecifier;
        this.moduleImport = null;
        this.importClause = null;
    }

    public Type getType() {
        return type;
    }

    public ModuleImport getModuleImport() {
        return moduleImport;
    }

    public ImportClause getImportClause() {
        return importClause;
    }

    public String getModuleSpecifier() {
        return moduleSpecifier;
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
