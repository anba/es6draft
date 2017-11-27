/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules.loader;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;

/**
 * 
 */
public abstract class AbstractURLModuleLoader<MODULE extends ModuleRecord> extends AbstractModuleLoader<MODULE> {
    private final URI baseDirectory;

    protected AbstractURLModuleLoader(RuntimeContext context) {
        super(context);
        this.baseDirectory = context.getBaseDirectory().toUri();
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
    protected final URLModuleSource loadSource(SourceIdentifier identifier) throws MalformedURLException {
        if (!(identifier instanceof URLSourceIdentifier)) {
            throw new IllegalArgumentException();
        }
        URLSourceIdentifier sourceId = (URLSourceIdentifier) identifier;
        URL resolvedURL = baseDirectory.resolve(sourceId.toUri()).toURL();
        String sourceName = baseDirectory.relativize(sourceId.toUri()).toString();
        return new URLModuleSource(resolvedURL, sourceName);
    }

    @Override
    public URLSourceIdentifier normalizeName(String unnormalizedName, SourceIdentifier referrerId)
            throws MalformedNameException {
        URI referrerURI = referrerId != null ? referrerId.toUri() : null;
        URI normalized = SourceIdentifiers.normalize(unnormalizedName, referrerURI, baseDirectory);
        return new URLSourceIdentifier(normalized);
    }
}
