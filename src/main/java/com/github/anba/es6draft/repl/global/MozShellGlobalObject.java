/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.NativeCode;

/**
 * Global object class with support for some moz-shell functions
 */
public final class MozShellGlobalObject extends ShellGlobalObject {
    public MozShellGlobalObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initializeScripted() throws IOException, URISyntaxException {
        NativeCode.load(getRealm(), "mozlegacy.js");
    }

    @Override
    public void initializeExtensions() {
        createGlobalProperties(new BaseShellFunctions(), BaseShellFunctions.class);
        createGlobalProperties(new MozShellFunctions(), MozShellFunctions.class);
    }
}
