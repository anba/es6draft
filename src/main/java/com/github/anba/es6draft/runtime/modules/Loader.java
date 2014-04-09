/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import java.util.LinkedHashMap;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.LinkedMap;
import com.github.anba.es6draft.runtime.objects.modules.LoaderObject;

/**
 * <h1>15 ECMAScript Language: Modules and Scripts</h1><br>
 * <h2>15.2 Modules</h2><br>
 * <h3>15.2.3 Runtime Semantics: Loader State</h3>
 * <ul>
 * <li>15.2.3.1 Loader Records and Loader Objects
 * </ul>
 */
public final class Loader {
    /** [[Realm]] */
    private final Realm realm;

    /** [[Modules]] */
    private final LinkedMap<String, ModuleLinkage> modules;

    /** [[Loads]] */
    private final LinkedHashMap<String, Load> loads;

    /** [[LoaderObj]] */
    private final LoaderObject loaderObj;

    public Loader(Realm realm, LoaderObject loaderObj) {
        this.realm = realm;
        this.modules = new LinkedMap<>();
        this.loads = new LinkedHashMap<>();
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
     * [[Modules]]
     *
     * @return the map from module names to module linkage records
     */
    public LinkedMap<String, ModuleLinkage> getModules() {
        return modules;
    }

    /**
     * [[Loads]]
     *
     * @return the map from module name to load records
     */
    public LinkedHashMap<String, Load> getLoads() {
        return loads;
    }

    /**
     * [[LoaderObj]]
     *
     * @return the loader script object
     */
    public LoaderObject getLoaderObj() {
        return loaderObj;
    }

    /**
     * 15.2.3.1.1 CreateLoaderRecord(realm, object) Abstract Operation
     * 
     * @param realm
     *            the realm instance
     * @param object
     *            the loader script object
     * @return the new loader record
     */
    public static Loader CreateLoader(Realm realm, LoaderObject object) {
        /* steps 1-6 */
        return new Loader(realm, object);
    }
}
