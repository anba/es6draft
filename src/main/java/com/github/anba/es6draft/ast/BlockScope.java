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
 * Scope class for nodes with block scope semantics
 */
public interface BlockScope extends Scope {
    /**
     * Returns the set of lexically declared names.
     */
    Set<String> lexicallyDeclaredNames();

    /**
     * Returns the list of lexically scoped declarations.
     */
    List<Declaration> lexicallyScopedDeclarations();
}
