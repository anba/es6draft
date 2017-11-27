/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.io.IOException;
import java.util.Collection;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.ModuleSource;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;

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
     * Defines a new module record in this module loader.
     * 
     * @param sourceId
     *            the source identifier
     * @param moduleSource
     *            the module source
     * @return the parsed module record
     * @throws IOException
     *             if there was any I/O error
     */
    ModuleRecord defineUnlinked(SourceIdentifier sourceId, ModuleSource moduleSource) throws IOException;

    /**
     * Returns an unmodifiable collection of the currently loaded modules.
     * 
     * @return the module collection
     */
    Collection<MODULE> getModules();
}
