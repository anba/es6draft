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

    /**
     * Returns the ordered set of module requests for this module scope.
     * 
     * @return the module requests
     */
    Set<String> getModuleRequests();

    /**
     * Returns an unordered set of exported bindings of this module scope
     * 
     * @return the exported bindings
     */
    Set<String> getExportBindings();
}
