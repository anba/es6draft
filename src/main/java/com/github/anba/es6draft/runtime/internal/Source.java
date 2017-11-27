/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.nio.file.Path;

import com.github.anba.es6draft.runtime.modules.SourceIdentifier;
import com.github.anba.es6draft.runtime.modules.loader.FileSourceIdentifier;

/**
 * Class representing script source code information.
 */
public final class Source {
    private final SourceIdentifier sourceId;
    private final Path file;
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
        this((SourceIdentifier) null, name, line);
    }

    /**
     * Constructs a new {@link Source} object.
     * 
     * @param sourceId
     *            the source identifier
     * @param name
     *            the source name
     * @param line
     *            the source start line offset
     */
    public Source(SourceIdentifier sourceId, String name, int line) {
        this.sourceId = sourceId;
        this.file = null;
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
        assert file.isAbsolute() : "File not absolute: " + file.toString();
        this.sourceId = new FileSourceIdentifier(file);
        this.file = file;
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
        this.sourceId = base != null ? base.sourceId : null;
        this.file = base != null ? base.file : null;
        this.name = name;
        this.line = line;
    }

    /**
     * Returns the source identifier if available.
     * 
     * @return the source identifier or {@code null} if not available
     */
    public SourceIdentifier getSourceId() {
        return sourceId;
    }

    /**
     * Returns the script file path if available.
     * 
     * @return the source file or {@code null} if not available
     */
    public Path getFile() {
        return file;
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
        if (file == null) {
            return String.format("Source {name='%s', line=%d}", name, line);
        }
        return String.format("Source {name='%s', file='%s', line=%d}", name, file, line);
    }
}
