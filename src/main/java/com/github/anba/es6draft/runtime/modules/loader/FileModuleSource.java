/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.ModuleSource;

/**
 * 
 */
public final class FileModuleSource implements ModuleSource {
    private final FileSourceIdentifier sourceId;
    private final Path sourceFile;

    public FileModuleSource(FileSourceIdentifier sourceId, Path sourceFile) {
        this.sourceId = sourceId;
        this.sourceFile = sourceFile;
    }

    @Override
    public String sourceCode() throws IOException {
        return new String(Files.readAllBytes(sourceFile), StandardCharsets.UTF_8);
    }

    @Override
    public Source toSource() {
        return new Source(sourceFile, sourceId.getFile(), 1);
    }
}
