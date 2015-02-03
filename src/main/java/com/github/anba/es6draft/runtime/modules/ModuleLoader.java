/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

/**
 * 
 */
public interface ModuleLoader {
    /**
     * Returns the normalized module identifier.
     * 
     * @param unnormalizedName
     *            the unnormalized module name
     * @param referrerId
     *            the identifier of the including module or {@code null}
     * @return the normalized module identifier or {@code null} if the name cannot be normalized
     */
    SourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId);

    /**
     * Retrieves the source code for the requested module.
     * 
     * @param moduleId
     *            the module identifier
     * @return the module source code
     * @throws IllegalArgumentException
     *             if the source identifier cannot be processed by this loader
     */
    ModuleSource getSource(SourceIdentifier moduleId) throws IllegalArgumentException;
}
