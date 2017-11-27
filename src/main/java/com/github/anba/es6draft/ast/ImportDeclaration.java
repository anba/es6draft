/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.2 Modules</h2>
 * <ul>
 * <li>15.2.2 Imports
 * </ul>
 */
public final class ImportDeclaration extends ModuleItem {
    private final Type type;
    private final ImportClause importClause;
    private final String moduleSpecifier;

    /**
     * The import declaration type.
     */
    public enum Type {
        /**
         * Import type: {@code import <thing> from module}
         */
        ImportFrom,

        /**
         * Import type: {@code import module}
         */
        ImportModule
    }

    public ImportDeclaration(long beginPosition, long endPosition, ImportClause importClause, String moduleSpecifier) {
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

    /**
     * Returns the import declaration type.
     * 
     * @return the import type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the optional import clause of this import declaration.
     * 
     * @return the import clause or {@code null}
     */
    public ImportClause getImportClause() {
        return importClause;
    }

    /**
     * Returns the module specifier of this import declaration.
     * 
     * @return the module specifier
     */
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
