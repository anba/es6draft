/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.webkit;

import java.io.IOException;
import java.util.Objects;

import com.github.anba.es6draft.repl.functions.BaseShellFunctions;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.RealmData;
import com.github.anba.es6draft.runtime.internal.ScriptLoading;

/**
 * 
 */
final class WebKitTestRealmData extends RealmData {
    WebKitTestRealmData(Realm realm) {
        super(realm);
    }

    static void testLoadInitializationScript() {
        Objects.requireNonNull(WebKitTestRealmData.class.getResource("/scripts/webkitlegacy.js"));
    }

    @Override
    public void initializeScripted() throws IOException {
        ScriptLoading.evalNative(getRealm(), "webkitlegacy.js");
    }

    @Override
    public void initializeExtensions() {
        getRealm().createGlobalProperties(new BaseShellFunctions(), BaseShellFunctions.class);
        getRealm().createGlobalProperties(new TestingFunctions(), TestingFunctions.class);
    }
}
