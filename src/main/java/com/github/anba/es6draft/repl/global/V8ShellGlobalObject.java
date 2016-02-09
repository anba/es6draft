/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.NativeCode;

/**
 * Global object class with support for some v8-shell functions
 */
public final class V8ShellGlobalObject extends ShellGlobalObject {
    public V8ShellGlobalObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initializeScripted() throws IOException, URISyntaxException, ParserException, CompilationException {
        NativeCode.load(getRealm(), "v8legacy.js");
    }

    @Override
    public void initializeExtensions() {
        createGlobalProperties(new BaseShellFunctions(), BaseShellFunctions.class);
        createGlobalProperties(new V8ShellFunctions(), V8ShellFunctions.class);
    }
}
