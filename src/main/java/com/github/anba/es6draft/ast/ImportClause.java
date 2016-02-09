/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.2 Modules</h2>
 * <ul>
 * <li>15.2.2 Imports
 * </ul>
 */
public final class ImportClause extends AstNode {
    private final BindingIdentifier defaultEntry;
    private final List<ImportSpecifier> namedImports;
    private final BindingIdentifier nameSpace;

    public ImportClause(long beginPosition, long endPosition, BindingIdentifier defaultEntry,
            List<ImportSpecifier> namedImports, BindingIdentifier nameSpace) {
        super(beginPosition, endPosition);
        this.defaultEntry = defaultEntry;
        this.namedImports = namedImports;
        this.nameSpace = nameSpace;
    }

    /**
     * Returns the optional default import entry.
     * 
     * @return the default import or {@code null}
     */
    public BindingIdentifier getDefaultEntry() {
        return defaultEntry;
    }

    /**
     * Returns the list of imported names.
     * 
     * @return the list of named imports
     */
    public List<ImportSpecifier> getNamedImports() {
        return namedImports;
    }

    /**
     * Returns the optional namespace import binding name.
     * 
     * @return the namespace import name or {@code null}
     */
    public BindingIdentifier getNameSpace() {
        return nameSpace;
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
