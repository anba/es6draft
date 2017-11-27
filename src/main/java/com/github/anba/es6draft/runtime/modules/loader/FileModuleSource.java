/**
 * Copyright (c) André Bargull
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
    private final Path sourceFile;
    private final String sourceName;

    public FileModuleSource(Path sourceFile, String sourceName) {
        this.sourceFile = sourceFile;
        this.sourceName = sourceName;
    }

    @Override
    public String sourceCode() throws IOException {
        return new String(Files.readAllBytes(sourceFile), StandardCharsets.UTF_8);
    }

    @Override
    public Source toSource() {
        return new Source(sourceFile, sourceName, 1);
    }
}
