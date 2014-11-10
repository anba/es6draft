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
    private final ImportClause importClause;
    private final String moduleSpecifier;

    public enum Type {
        ImportFrom, ImportModule
    }

    public ImportDeclaration(long beginPosition, long endPosition, ImportClause importClause,
            String moduleSpecifier) {
        super(beginPosition, endPosition);
        this.type = Type.ImportFrom;
        this.importClause = importClause;
        this.moduleSpecifier = moduleSpecifier;
    }

    public ImportDeclaration(long beginPosition, long endPosition, String moduleSpecifier) {
        super(beginPosition, endPosition);
        this.type = Type.ImportModule;
        this.importClause = null;
        this.moduleSpecifier = moduleSpecifier;
    }

    public Type getType() {
        return type;
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

    @Override
    public <V> void accept(VoidNodeVisitor<V> visitor, V value) {
        visitor.visit(this, value);
    }
}
