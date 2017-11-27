/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules.loader;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;

/**
 * 
 */
public abstract class AbstractFileModuleLoader<MODULE extends ModuleRecord> extends AbstractModuleLoader<MODULE> {
    private final Path baseDirectory;
    private final URI baseDirectoryURI;

    protected AbstractFileModuleLoader(RuntimeContext context) {
        super(context);
        this.baseDirectory = context.getBaseDirectory().toAbsolutePath();
        this.baseDirectoryURI = context.getBaseDirectory().toUri();
    }

    protected final Path getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException
     *             if the source identifier is not a {@link FileSourceIdentifier}
     */
    @Override
    protected final FileModuleSource loadSource(SourceIdentifier identifier) {
        if (!(identifier instanceof FileSourceIdentifier)) {
            throw new IllegalArgumentException();
        }
        FileSourceIdentifier sourceId = (FileSourceIdentifier) identifier;
        URI resolvedURI = baseDirectoryURI.resolve(sourceId.toUri());
        String sourceName = baseDirectoryURI.relativize(sourceId.toUri()).toString();
        return new FileModuleSource(Paths.get(resolvedURI), sourceName);
    }

    @Override
    public FileSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId)
            throws MalformedNameException {
        URI referrerURI = referrerId != null ? referrerId.toUri() : null;
        URI normalized = SourceIdentifiers.normalize(unnormalizedName, referrerURI, baseDirectoryURI);
        return new FileSourceIdentifier(normalized);
    }
}
