/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.modules.Loader;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.? Loader Objects</h2>
 * <ul>
 * <li>26.?.4 Properties of Reflect.Loader Instances
 * </ul>
 */
public class LoaderObject extends OrdinaryObject {
    /** [[Loader]] */
    private Loader loader;

    /**
     * Constructs a new Loader object.
     * 
     * @param realm
     *            the realm object
     */
    protected LoaderObject(Realm realm) {
        super(realm);
    }

    /**
     * [[Loader]]
     *
     * @return the loader record
     */
    public final Loader getLoader() {
        return loader;
    }

    /**
     * [[Loader]]
     *
     * @param loader
     *            the new loader record
     */
    protected final void setLoader(Loader loader) {
        assert this.loader == null && loader != null : "LoaderObject already initialized";
        this.loader = loader;
    }
}
