/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>14 Scripts and Modules</h1><br>
 * <h2>14.2 Modules</h2>
 */
public class ImportSpecifier extends AstNode {
    private String externalName;
    private String localName;

    public ImportSpecifier(String externalName, String localName) {
        this.externalName = externalName;
        this.localName = localName;
    }

    public String getExternalName() {
        return externalName;
    }

    public String getLocalName() {
        return localName;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
