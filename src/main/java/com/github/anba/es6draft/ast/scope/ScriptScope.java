/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.scope;

import java.util.Set;

import com.github.anba.es6draft.ast.Script;

/**
 * Scope class for {@link Script} objects
 */
public interface ScriptScope extends TopLevelScope {
    @Override
    Script getNode();

    /**
     * Returns the set of variable declared names in function and for-of statements. This
     * information is only tracked for eval scripts.
     * 
     * @return the variable declared names
     */
    Set<Name> varFunctionAndForOfDeclaredNames();
}
