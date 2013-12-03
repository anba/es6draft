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
public class ImportSpecifier extends AstNode {
    private String importName;
    private BindingIdentifier localName;

    public ImportSpecifier(long beginPosition, long endPosition, String importName,
            BindingIdentifier localName) {
        super(beginPosition, endPosition);
        this.importName = importName;
        this.localName = localName;
    }

    public String getImportName() {
        return importName;
    }

    public BindingIdentifier getLocalName() {
        return localName;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
