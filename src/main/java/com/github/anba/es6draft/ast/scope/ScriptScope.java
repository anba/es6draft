/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.scope;

import java.util.List;
import java.util.Set;

import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.Script;

/**
 * Scope class for {@link Script} objects
 */
public interface ScriptScope extends TopLevelScope {
    @Override
    Script getNode();

    /**
     * Returns the set of variable declared names in for-of statements. This information is only
     * tracked for eval scripts.
     * 
     * @return the variable declared names
     */
    Set<Name> varForOfDeclaredNames();

    /**
     * Returns the script's web legacy block-level function declarations.
     * 
     * @return the function declarations
     */
    List<FunctionDeclaration> blockFunctions();
}
