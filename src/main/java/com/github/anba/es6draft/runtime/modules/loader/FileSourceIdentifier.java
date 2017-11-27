/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules.loader;

import java.net.URI;
import java.nio.file.Path;

import com.github.anba.es6draft.runtime.modules.SourceIdentifier;

/**
 * 
 */
public final class FileSourceIdentifier implements SourceIdentifier {
    private final URI uri;

    /**
     * Constructs a new file source identifier.
     * 
     * @param unnormalizedName
     *            the normalized module name
     */
    FileSourceIdentifier(URI uri) {
        this.uri = uri.normalize();
    }

    /**
     * Constructs a new file source identifier.
     * 
     * @param path
     *            the module file
     */
    public FileSourceIdentifier(Path path) {
        this.uri = path.toAbsolutePath().normalize().toUri();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SourceIdentifier) {
            return toUri().equals(((SourceIdentifier) obj).toUri());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public String toString() {
        return uri.toString();
    }

    @Override
    public URI toUri() {
        return uri;
    }
}
