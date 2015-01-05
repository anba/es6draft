/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.nio.file.Path;

/**
 * Base class to store test information
 */
public class TestInfo {
    private final Path baseDir;
    private final Path script;
    private boolean enabled = true;

    public TestInfo(Path basedir, Path file) {
        this.baseDir = basedir;
        this.script = basedir.relativize(file);
    }

    public final Path getBaseDir() {
        return baseDir;
    }

    public final Path getScript() {
        return script;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return script.toString();
    }

    public final Path toFile() {
        return baseDir.resolve(script);
    }

    public String toModuleName() {
        return baseDir.toUri().relativize(toFile().toUri()).toString();
    }

    public boolean isModule() {
        return script.getFileName().toString().endsWith(".jsm");
    }
}
