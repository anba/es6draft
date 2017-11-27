/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mozilla;

import java.io.IOException;
import java.util.Objects;

import com.github.anba.es6draft.repl.functions.AtomicsTestFunctions;
import com.github.anba.es6draft.repl.functions.BaseShellFunctions;
import com.github.anba.es6draft.repl.functions.MozShellFunctions;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.RealmData;
import com.github.anba.es6draft.runtime.internal.ScriptLoading;

/**
 *
 */
final class MozTestRealmData extends RealmData {
    MozTestRealmData(Realm realm) {
        super(realm);
    }

    static void testLoadInitializationScript() {
        Objects.requireNonNull(MozTestRealmData.class.getResource("/scripts/mozlegacy.js"));
    }

    @Override
    public void initializeScripted() throws IOException {
        ScriptLoading.evalNative(getRealm(), "mozlegacy.js");
    }

    @Override
    public void initializeExtensions() {
        getRealm().createGlobalProperties(new BaseShellFunctions(), BaseShellFunctions.class);
        getRealm().createGlobalProperties(new MozShellFunctions(), MozShellFunctions.class);
        getRealm().createGlobalProperties(new TestingFunctions(), TestingFunctions.class);
        getRealm().createGlobalProperties(new AtomicsTestFunctions(), AtomicsTestFunctions.class);
    }
}
