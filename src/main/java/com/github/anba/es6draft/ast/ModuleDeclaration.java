/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>14 Scripts and Modules</h1><br>
 * <h2>14.2 Modules</h2>
 */
public class ModuleDeclaration extends StatementListItem implements ScopedNode {
    private String moduleName;
    private List<StatementListItem> body;
    private String identifier;
    private FunctionScope scope;

    public ModuleDeclaration(String moduleName, List<StatementListItem> body, FunctionScope scope) {
        this.moduleName = moduleName;
        this.body = body;
        this.scope = scope;
    }

    public ModuleDeclaration(String identifier, String moduleName, FunctionScope scope) {
        this.identifier = identifier;
        this.moduleName = moduleName;
        this.scope = scope;
    }

    public String getModuleName() {
        return moduleName;
    }

    public List<StatementListItem> getBody() {
        return body;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public FunctionScope getScope() {
        return scope;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
