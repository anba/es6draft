/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.Set;

/**
 * Scope class for {@link Module} objects
 */
public interface ModuleScope extends TopLevelScope {
    @Override
    Module getNode();

    Set<String> getModuleRequests();

    Set<String> getExportBindings();
}
