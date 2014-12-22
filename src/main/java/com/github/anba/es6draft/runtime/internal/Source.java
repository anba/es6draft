/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class representing script source code information.
 */
public final class Source {
    private final Path filePath;
    private final String fileName;
    private final String name;
    private final int line;

    /**
     * Constructs a new {@link Source} object.
     * 
     * @param name
     *            the source name
     * @param line
     *            the source start line offset
     */
    public Source(String name, int line) {
        this.filePath = null;
        this.fileName = null;
        this.name = name;
        this.line = line;
    }

    /**
     * Constructs a new {@link Source} object.
     * 
     * @param file
     *            the source file
     * @param name
     *            the source name
     * @param line
     *            the source start line offset
     */
    public Source(Path file, String name, int line) {
        assert file == null || file.isAbsolute() : "File not absolute: " + file.toString();
        this.filePath = file;
        this.fileName = null;
        this.name = name;
        this.line = line;
    }

    /**
     * Constructs a new {@link Source} object.
     * 
     * @param base
     *            the base source object
     * @param name
     *            the source name
     * @param line
     *            the source start line offset
     */
    public Source(Source base, String name, int line) {
        this.filePath = base != null ? base.filePath : null;
        this.fileName = base != null ? base.fileName : null;
        this.name = name;
        this.line = line;
    }

    /**
     * Constructs a new {@link Source} object.
     * 
     * @param file
     *            the source file
     * @param name
     *            the source name
     * @param line
     *            the source start line offset
     */
    /*package*/Source(String file, String name, int line) {
        this.filePath = null;
        this.fileName = file;
        this.name = name;
        this.line = line;
    }

    /**
     * Returns the script file path if available.
     * 
     * @return the source file or {@code null} if not available
     */
    public Path getFile() {
        if (filePath != null) {
            return filePath;
        }
        if (fileName != null) {
            return Paths.get(fileName);
        }
        return null;
    }

    /**
     * Returns the script file path if available.
     * 
     * @return the source file or {@code null} if not available
     */
    public String getFileString() {
        if (filePath != null) {
            return filePath.toString();
        }
        if (fileName != null) {
            return fileName;
        }
        return null;
    }

    /**
     * Returns the descriptive name of the source code.
     * 
     * @return the source name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the source line offset.
     * 
     * @return the source line offset
     */
    public int getLine() {
        return line;
    }

    @Override
    public String toString() {
        String file = getFileString();
        if (file == null) {
            return String.format("Source {name='%s', line=%d}", name, line);
        }
        return String.format("Source {name='%s', file='%s', line=%d}", name, file, line);
    }
}
