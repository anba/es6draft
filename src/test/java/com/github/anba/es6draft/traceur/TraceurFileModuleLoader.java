/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.traceur;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import com.github.anba.es6draft.runtime.internal.FileModuleLoader;

/**
 * 
 */
final class TraceurFileModuleLoader extends FileModuleLoader {
    TraceurFileModuleLoader(Path baseDirectory) {
        super(baseDirectory);
    }

    @Override
    public String normalizeName(String unnormalizedName, String referrerId) {
        try {
            String normalizedName = super.normalizeName(unnormalizedName, referrerId);
            if (!normalizedName.endsWith(".js")) {
                normalizedName += ".js";
            }
            return normalizedName;
        } catch (InvalidPathException e) {
            return null;
        }
    }
}
