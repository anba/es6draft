/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mozilla;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.global.BaseShellFunctions;
import com.github.anba.es6draft.repl.global.MozShellFunctions;
import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.NativeCode;

/**
 *
 */
final class MozTestGlobalObject extends ShellGlobalObject {
    MozTestGlobalObject(Realm realm) {
        super(realm);
    }

    static void testLoadInitializationScript() throws IOException {
        NativeCode.getScriptURL("mozlegacy.js");
    }

    @Override
    public void initializeScripted() throws IOException, URISyntaxException, ParserException, CompilationException {
        NativeCode.load(getRealm(), "mozlegacy.js");
    }

    @Override
    public void initializeExtensions() {
        createGlobalProperties(new BaseShellFunctions(), BaseShellFunctions.class);
        createGlobalProperties(new MozShellFunctions(), MozShellFunctions.class);
        createGlobalProperties(new TestingFunctions(), TestingFunctions.class);
    }
}
