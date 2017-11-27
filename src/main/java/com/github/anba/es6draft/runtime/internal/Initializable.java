/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.Realm;

/**
 * Interface for objects with post-construction initialization.
 */
public interface Initializable {
    /**
     * The initialization method for this object.
     * 
     * @param realm
     *            the realm instance
     */
    void initialize(Realm realm);
}
