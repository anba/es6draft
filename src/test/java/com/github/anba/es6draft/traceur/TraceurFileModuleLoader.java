/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.traceur;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.anba.es6draft.runtime.internal.FileModuleLoader;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;

/**
 * 
 */
final class TraceurFileModuleLoader extends FileModuleLoader {
    TraceurFileModuleLoader(Path baseDirectory) {
        super(baseDirectory);
    }

    @Override
    public FileSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId) {
        try {
            FileSourceIdentifier normalizedName = super.normalizeName(unnormalizedName, referrerId);
            Path fileName = normalizedName.getPath().getFileName();
            if (!fileName.toString().endsWith(".js")) {
                Path path = Paths.get(normalizedName.getPath() + ".js");
                normalizedName = new FileSourceIdentifier(path);
            }
            return normalizedName;
        } catch (InvalidPathException e) {
            return null;
        }
    }
}
