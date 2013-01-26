/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.test262.environment;

import java.io.IOException;
import java.io.InputStream;

import com.github.anba.test262.util.ScriptErrorMatcher;

/**
 * 
 */
public interface Environment<GLOBAL extends GlobalObject> {
    void eval(String sourceName, InputStream source) throws IOException;

    GLOBAL global();

    Class<?>[] exceptions();

    ScriptErrorMatcher<? extends Throwable> matcher(String errorType);
}
