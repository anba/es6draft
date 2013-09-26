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
public class ExportSpecifierSet extends AstNode {
    private List<ExportSpecifier> exports;
    private String sourceModule;

    public ExportSpecifierSet(String sourceModule) {
        this.sourceModule = sourceModule;
    }

    public ExportSpecifierSet(List<ExportSpecifier> exports, String sourceModule) {
        this.exports = exports;
        this.sourceModule = sourceModule;
    }

    public List<ExportSpecifier> getExports() {
        return exports;
    }

    public String getSourceModule() {
        return sourceModule;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
