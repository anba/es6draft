/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.scope;

import com.github.anba.es6draft.ast.Module;

/**
 * Scope class for {@link Module} objects
 */
public interface ModuleScope extends TopLevelScope {
    @Override
    Module getNode();

    /**
     * Adds an implicit binding to the module scope.
     * 
     * @param name
     *            the binding name
     */
    void addImplicitBinding(Name name);
}
