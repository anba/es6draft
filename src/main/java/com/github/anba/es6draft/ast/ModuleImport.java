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
public final class ModuleImport extends AstNode {
    private final BindingIdentifier importedBinding;
    private final String moduleSpecifier;

    public ModuleImport(long beginPosition, long endPosition, BindingIdentifier importedBinding,
            String moduleSpecifier) {
        super(beginPosition, endPosition);
        this.importedBinding = importedBinding;
        this.moduleSpecifier = moduleSpecifier;
    }

    public BindingIdentifier getImportedBinding() {
        return importedBinding;
    }

    public String getModuleSpecifier() {
        return moduleSpecifier;
    }

    @Override
    public <R, V> R accept(NodeVisitor<R, V> visitor, V value) {
        return visitor.visit(this, value);
    }
}
