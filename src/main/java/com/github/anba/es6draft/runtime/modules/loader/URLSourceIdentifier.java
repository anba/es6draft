/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules.loader;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.SourceIdentifier;

/**
 * 
 */
public final class URLSourceIdentifier implements SourceIdentifier {
    private final URI uri;

    /**
     * Constructs a new url source identifier.
     * 
     * @param unnormalizedName
     *            the unnormalized module name
     * @param referrerId
     *            the identifier of the including module or {@code null}
     * @throws MalformedNameException
     *             if the name cannot be normalized
     */
    public URLSourceIdentifier(String unnormalizedName, SourceIdentifier referrerId) throws MalformedNameException {
        this.uri = SourceIdentifiers.normalize(unnormalizedName, referrerId);
    }

    /**
     * Constructs a new url source identifier.
     * 
     * @param url
     *            the module url
     * @throws URISyntaxException
     *             if the url cannot be converted to a uri
     */
    public URLSourceIdentifier(URL url) throws URISyntaxException {
        this.uri = url.toURI();
    }

    public URL getURL() throws MalformedURLException {
        return uri.toURL();
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
