/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.promise;

import com.github.anba.es6draft.repl.functions.BaseShellFunctions;
import com.github.anba.es6draft.repl.functions.ShellFunctions;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.RealmData;

/**
 *
 */
public final class PromiseTestRealmData extends RealmData {
    public PromiseTestRealmData(Realm realm) {
        super(realm);
    }

    @Override
    public void initializeExtensions() {
        getRealm().createGlobalProperties(new BaseShellFunctions(), BaseShellFunctions.class);
        getRealm().createGlobalProperties(new ShellFunctions(), ShellFunctions.class);
    }
}
