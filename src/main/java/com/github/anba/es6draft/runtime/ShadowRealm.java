/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import java.util.Map;

import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;

/**
 * Minimum Realm interface to support module name resolution and parsing when no actual
 * {@link Realm} instance is available.
 */
public interface ShadowRealm {
    /**
     * Returns the module loader.
     * 
     * @return the module loader
     */
    ModuleLoader getModuleLoader();

    /**
     * Returns the script loader.
     * 
     * @return the script loader
     */
    ScriptLoader getScriptLoader();

    /**
     * [[modules]]
     * 
     * @return the map of resolved modules
     */
    Map<SourceIdentifier, ModuleRecord> getModules();
}
