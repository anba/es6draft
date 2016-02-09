/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules.loader;

import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;

/**
 * 
 */
public final class FileSourceIdentifier implements SourceIdentifier {
    private final String file;

    /**
     * Constructs a new file source identifier.
     * 
     * @param unnormalizedName
     *            the unnormalized module name
     * @param referrerId
     *            the identifier of the including module or {@code null}
     * @throws MalformedNameException
     *             if the name cannot be normalized
     */
    public FileSourceIdentifier(String unnormalizedName, SourceIdentifier referrerId) throws MalformedNameException {
        URI normalized = SourceIdentifiers.normalize(unnormalizedName, referrerId);
        this.file = normalized.getPath();
    }

    /**
     * Constructs a new file source identifier.
     * 
     * @param base
     *            the base directory
     * @param path
     *            the module file
     */
    public FileSourceIdentifier(Path base, Path path) {
        this.file = base.toAbsolutePath().toUri().relativize(base.resolve(path).toUri()).toString();
    }

    /*package*/String getFile() {
        return file;
    }

    public Path getPath() throws InvalidPathException {
        return Paths.get(file);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileSourceIdentifier) {
            return file.equals(((FileSourceIdentifier) obj).file);
        }
        if (obj instanceof SourceIdentifier) {
            return toUri().equals(((SourceIdentifier) obj).toUri());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public String toString() {
        return file;
    }

    @Override
    public URI toUri() {
        return URI.create(file);
    }
}
