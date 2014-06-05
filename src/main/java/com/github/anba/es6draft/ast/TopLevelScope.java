/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;
import java.util.Set;

/**
 * Scope class for {@link FunctionNode} and {@link Script} objects.
 */
public interface TopLevelScope extends Scope {
    /**
     * Always returns <code>null</code> for function scopes.
     */
    @Override
    Scope getParent();

    /**
     * Returns the scope which encloses this scope.
     * 
     * @return the enclosing scope
     */
    Scope getEnclosingScope();

    @Override
    TopLevelNode<?> getNode();

    /**
     * Returns the set of lexically declared names.
     * 
     * @return the lexically declared names
     */
    Set<String> lexicallyDeclaredNames();

    /**
     * Returns the list of lexically scoped declarations.
     * 
     * @return the lexically scoped declarations
     */
    List<Declaration> lexicallyScopedDeclarations();

    /**
     * Returns the set of variable declared names.
     * 
     * @return the variables declared names
     */
    Set<String> varDeclaredNames();

    /**
     * Returns the list of variable scoped declarations.
     * 
     * @return the variable scoped declarations
     */
    List<StatementListItem> varScopedDeclarations();
}
