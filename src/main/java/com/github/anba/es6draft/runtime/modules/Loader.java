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

    /** [[Realm]] */
    public Realm getRealm() {
        return realm;
    }

    /** [[Modules]] */
    public LinkedMap<String, ModuleLinkage> getModules() {
        return modules;
    }

    /** [[Loads]] */
    public LinkedHashMap<String, Load> getLoads() {
        return loads;
    }

    /** [[LoaderObj]] */
    public LoaderObject getLoaderObj() {
        return loaderObj;
    }

    /**
     * 15.2.3.1.1 CreateLoaderRecord(realm, object) Abstract Operation
     */
    public static Loader CreateLoader(Realm realm, LoaderObject object) {
        /* steps 1-6 */
        return new Loader(realm, object);
    }
}
