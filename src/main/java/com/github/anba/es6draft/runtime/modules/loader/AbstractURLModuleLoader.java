/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules.loader;

import java.net.URI;

import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;

/**
 * 
 */
public abstract class AbstractURLModuleLoader<MODULE extends ModuleRecord> extends
        AbstractModuleLoader<MODULE> {
    private final URI baseDirectory;

    public AbstractURLModuleLoader(URI baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    protected final URI getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException
     *             if the source identifier is not a {@link URLSourceIdentifier}
     */
    @Override
    protected final URLModuleSource loadSource(SourceIdentifier identifier) {
        if (!(identifier instanceof URLSourceIdentifier)) {
            throw new IllegalArgumentException();
        }
        URLSourceIdentifier sourceId = (URLSourceIdentifier) identifier;
        return new URLModuleSource(sourceId);
    }

    @Override
    public URLSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId)
            throws MalformedNameException {
        return new URLSourceIdentifier(unnormalizedName, referrerId);
    }
}
