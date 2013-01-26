/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.test262.util;

/**
 * 
 */
public interface ScriptErrorMatcher<T extends Throwable> {
    Class<? extends T> exception();

    boolean matches(T error, String errorType);
}
