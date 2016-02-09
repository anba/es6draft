/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import java.net.URI;

/**
 *
 */
public interface SourceIdentifier {
    /**
     * <strong>{@link SourceIdentifier} implementations need to override this method!</strong>
     * <p>
     * {@inheritDoc}
     */
    @Override
    boolean equals(Object obj);

    /**
     * <strong>{@link SourceIdentifier} implementations need to override this method!</strong>
     * <p>
     * {@inheritDoc}
     */
    @Override
    int hashCode();

    /**
     * <strong>{@link SourceIdentifier} implementations need to override this method!</strong>
     * <p>
     * {@inheritDoc}
     */
    @Override
    String toString();

    /**
     * Returns the URI for this source identifier.
     * 
     * @return the URI for this source identifier
     */
    URI toUri();
}
