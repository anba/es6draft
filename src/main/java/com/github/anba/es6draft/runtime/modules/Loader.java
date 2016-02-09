/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.objects.reflect.LoaderObject;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1><br>
 * <h2>15.2 Modules</h2>
 * <ul>
 * <li>Loader Records and Loader Objects
 * </ul>
 */
public final class Loader {
    /** [[Realm]] */
    private final Realm realm;

    /** [[LoaderObj]] */
    private final LoaderObject loaderObj;

    public Loader(Realm realm, LoaderObject loaderObj) {
        this.realm = realm;
        this.loaderObj = loaderObj;
    }

    /**
     * [[Realm]]
     *
     * @return the realm instance
     */
    public Realm getRealm() {
        return realm;
    }

    /**
     * [[LoaderObj]]
     *
     * @return the loader script object
     */
    public LoaderObject getLoaderObj() {
        return loaderObj;
    }
}
