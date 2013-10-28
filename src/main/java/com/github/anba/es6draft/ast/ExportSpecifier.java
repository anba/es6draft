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
public class ExportSpecifier extends AstNode {
    private String localName;
    private String externalName;

    public ExportSpecifier(long beginPosition, long endPosition, String localName,
            String externalName) {
        super(beginPosition, endPosition);
        this.localName = localName;
        this.externalName = externalName;
    }

    public String getLocalName() {
        return localName;
    }

    public String getExternalName() {
        return externalName;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
