/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.util.Collection;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;

/**
 *
 */
public interface TestModuleLoader<MODULE extends ModuleRecord> extends ModuleLoader {
    /**
     * Defines a new module record in this module loader.
     * 
     * @param template
     *            the template module record
     * @param realm
     *            the realm instance
     */
    void defineFromTemplate(ModuleRecord template, Realm realm);

    /**
     * Returns an unmodifiable collection of the currently loaded modules.
     * 
     * @return the module collection
     */
    Collection<MODULE> getModules();
}
