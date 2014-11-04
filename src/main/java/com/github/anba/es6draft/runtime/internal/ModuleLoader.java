/**
 * Copyright (c) 2012-2014 André Bargull
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
     * Returns the normalized module name.
     * 
     * @param parentName
     *            the normalized name of the including module
     * @param unnormalizedName
     *            the unnormalized module name
     * @return the normalized module name or {@code null} if not a valid module name
     */
    public abstract String normalizeName(String parentName, String unnormalizedName);

    /**
     * Retrieves the source code for the requested module.
     * 
     * @param normalizedName
     *            the normalized module name
     * @return the module source code
     * @throws IOException
     *             if there was any I/O error
     */
    public abstract String getSource(String normalizedName) throws IOException;

    /**
     * Retrieves the source file for the requested module.
     * 
     * @param normalizedName
     *            the normalized module name
     * @return the module file or {@code null} if not available
     */
    public abstract Path getSourceFile(String normalizedName);
}
