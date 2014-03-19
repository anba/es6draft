/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.modules;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.modules.Loader.CreateLoader;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.modules.Loader;
import com.github.anba.es6draft.runtime.types.Intrinsics;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.4 The System Object</h2>
 */
public final class SystemObject extends LoaderObject implements Initialisable {
    public SystemObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        Loader loaderRecord = CreateLoader(cx.getRealm(), this);
        setLoader(loaderRecord);

        createProperties(cx, this, Properties.class);
    }

    /**
     * Properties of the System Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.LoaderPrototype;
    }
}
