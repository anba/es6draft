/**
 * Copyright (c) 2012-2015 André Bargull
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
public final class ImportSpecifier extends AstNode {
    private final String importName;
    private final BindingIdentifier localName;

    public ImportSpecifier(long beginPosition, long endPosition, String importName,
            BindingIdentifier localName) {
        super(beginPosition, endPosition);
        this.importName = importName;
        this.localName = localName;
    }

    /**
     * Returns the imported name.
     * 
     * @return the import name
     */
    public String getImportName() {
        return importName;
    }

    /**
     * Returns the local binding name.
     * 
     * @return the local name
     */
    public BindingIdentifier getLocalName() {
        return localName;
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
