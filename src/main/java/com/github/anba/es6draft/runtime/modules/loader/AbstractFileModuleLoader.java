/**
 * Copyright (c) 2012-2016 André Bargull
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
        this.baseDirectory = context.getBaseDirectory();
        this.baseDirectoryURI = context.getBaseDirectory().toUri();
    }

    public final Path getBaseDirectory() {
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
        Path path = Paths.get(baseDirectoryURI.resolve(sourceId.toUri()));
        return new FileModuleSource(sourceId, path);
    }

    @Override
    public FileSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId)
            throws MalformedNameException {
        return new FileSourceIdentifier(unnormalizedName, referrerId);
    }
}
