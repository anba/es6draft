/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.chakra;

import java.io.IOException;
import java.util.Objects;

import com.github.anba.es6draft.repl.functions.BaseShellFunctions;
import com.github.anba.es6draft.repl.functions.ShellFunctions;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.RealmData;
import com.github.anba.es6draft.runtime.internal.ScriptLoading;

/**
 * 
 */
final class ChakraTestRealmData extends RealmData {
    ChakraTestRealmData(Realm realm) {
        super(realm);
    }

    static void testLoadInitializationScript() {
        Objects.requireNonNull(ChakraTestRealmData.class.getResource("/scripts/chakralegacy.js"));
    }

    @Override
    public void initializeScripted() throws IOException {
        ScriptLoading.evalNative(getRealm(), "chakralegacy.js");
    }

    @Override
    public void initializeExtensions() {
        getRealm().createGlobalProperties(new BaseShellFunctions(), BaseShellFunctions.class);
        getRealm().createGlobalProperties(new ShellFunctions(), ShellFunctions.class);
        getRealm().createGlobalProperties(new TestingFunctions(), TestingFunctions.class);
    }
}
