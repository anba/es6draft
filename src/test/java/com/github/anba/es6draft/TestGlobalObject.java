/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import com.github.anba.es6draft.repl.global.AtomicsTestFunctions;
import com.github.anba.es6draft.repl.global.BaseShellFunctions;
import com.github.anba.es6draft.repl.global.ShellFunctions;
import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.runtime.Realm;

/**
 *
 */
public final class TestGlobalObject extends ShellGlobalObject {
    public TestGlobalObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initializeExtensions() {
        createGlobalProperties(new BaseShellFunctions(), BaseShellFunctions.class);
        createGlobalProperties(new ShellFunctions(), ShellFunctions.class);
        createGlobalProperties(new AtomicsTestFunctions(), AtomicsTestFunctions.class);
    }
}
