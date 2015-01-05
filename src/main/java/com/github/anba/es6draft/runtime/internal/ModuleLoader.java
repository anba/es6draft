/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 
 */
public abstract class ModuleLoader {
    // TODO: Share methods with ScriptLoader or change to interface?

    protected ModuleLoader() {
    }

    /**
     * Returns the normalized module identifier.
     * 
     * @param unnormalizedName
     *            the unnormalized module name
     * @param referrerId
     *            the identifier of the including module or {@code null}
     * @return the normalized module identifier or {@code null} if the name cannot be normalized
     */
    public abstract String normalizeName(String unnormalizedName, String referrerId);

    /**
     * Retrieves the source code for the requested module.
     * 
     * @param moduleId
     *            the module identifier
     * @return the module source code
     * @throws IOException
     *             if there was any I/O error
     */
    public abstract String getSource(String moduleId) throws IOException;

    /**
     * Retrieves the source file for the requested module.
     * 
     * @param moduleId
     *            the module identifier
     * @return the module file or {@code null} if not available
     */
    public abstract Path getSourceFile(String moduleId);
}
