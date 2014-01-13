/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.modules;

import java.util.LinkedHashMap;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.LinkedMap;
import com.github.anba.es6draft.runtime.modules.Load;
import com.github.anba.es6draft.runtime.modules.ModuleObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>1 Modules: Semantics</h1><br>
 * <h2>1.6 Loader Objects</h2>
 */
public class LoaderObject extends OrdinaryObject {
    /** [[Realm]] */
    private Realm realm;

    /** [[Modules]] */
    private LinkedMap<String, ModuleObject> modules;

    /** [[Loads]] */
    private LinkedHashMap<String, Load> loads;

    public LoaderObject(Realm realm) {
        super(realm);
    }

    /**
     * Initialises this {@link LoaderObject} instance
     */
    public void initialise(Realm realm) {
        assert this.realm == null && realm != null;
        this.realm = realm;
        this.modules = new LinkedMap<>();
        this.loads = new LinkedHashMap<>();
    }

    /** [[Realm]] */
    public Realm getRealm() {
        return realm;
    }

    /** [[Modules]] */
    public LinkedMap<String, ModuleObject> getModules() {
        return modules;
    }

    /** [[Loads]] */
    public LinkedHashMap<String, Load> getLoads() {
        return loads;
    }
}
