/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.scope;

import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.FormalParameter;
import com.github.anba.es6draft.ast.StatementListItem;

/**
 * Interface for parameter scopes.
 */
public interface ParameterScope extends Scope {
    @Override
    FormalParameter getNode();

    /**
     * Returns the set of variable declared names.
     * 
     * @return the variable declared names
     */
    Set<Name> varDeclaredNames();

    /**
     * Returns the list of variable scoped declarations.
     * 
     * @return the variable scoped declarations
     */
    List<StatementListItem> varScopedDeclarations();
}
