/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.v8;

import java.io.IOException;
import java.util.Objects;

import com.github.anba.es6draft.repl.functions.BaseShellFunctions;
import com.github.anba.es6draft.repl.functions.V8ShellFunctions;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.RealmData;
import com.github.anba.es6draft.runtime.internal.ScriptLoading;

/**
 * 
 */
final class V8TestRealmData extends RealmData {
    V8TestRealmData(Realm realm) {
        super(realm);
    }

    static void testLoadInitializationScript() {
        Objects.requireNonNull(V8TestRealmData.class.getResource("/scripts/v8legacy.js"));
    }

    @Override
    public void initializeScripted() throws IOException {
        ScriptLoading.evalNative(getRealm(), "v8legacy.js");
    }

    @Override
    public void initializeExtensions() {
        getRealm().createGlobalProperties(new BaseShellFunctions(), BaseShellFunctions.class);
        getRealm().createGlobalProperties(new V8ShellFunctions(), V8ShellFunctions.class);
        getRealm().createGlobalProperties(new TestingFunctions(), TestingFunctions.class);
    }
}
