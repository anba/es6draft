/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.scope;

import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.Declaration;

/**
 * Scope class for nodes with block scope semantics.
 */
public interface BlockScope extends Scope {
    /**
     * Returns the set of lexically declared names.
     * 
     * @return the lexically declared names
     */
    Set<Name> lexicallyDeclaredNames();

    /**
     * Returns the list of lexically scoped declarations.
     * 
     * @return the lexically scoped declarations
     */
    List<Declaration> lexicallyScopedDeclarations();
}
