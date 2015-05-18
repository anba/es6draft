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

import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.FileSourceIdentifier;
import com.github.anba.es6draft.util.TestFileModuleLoader;

/**
 * 
 */
final class TraceurFileModuleLoader extends TestFileModuleLoader {
    TraceurFileModuleLoader(ScriptLoader scriptLoader, Path baseDirectory) {
        super(scriptLoader, baseDirectory);
    }

    @Override
    public FileSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId)
            throws MalformedNameException {
        FileSourceIdentifier normalizedName = super.normalizeName(unnormalizedName, referrerId);
        try {
            Path fileName = normalizedName.getPath().getFileName();
            if (!fileName.toString().endsWith(".js")) {
                Path path = Paths.get(normalizedName.getPath() + ".js");
                normalizedName = new FileSourceIdentifier(getBaseDirectory(), path);
            }
            return normalizedName;
        } catch (InvalidPathException e) {
            throw new MalformedNameException(unnormalizedName);
        }
    }
}
