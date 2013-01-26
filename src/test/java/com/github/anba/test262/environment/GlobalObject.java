/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.test262.environment;

import java.io.IOException;

/**
 * Interface for canonical test262 tests
 * 
 */
public interface GlobalObject {
    /**
     * {@code $ERROR} function for canonical test262 tests
     */
    void error(String message);

    /**
     * {@code $FAIL} function for canonical test262 tests
     */
    void fail(String message);

    /**
     * {@code $PRINT} function for canonical test262 tests
     */
    void print(String message);

    /**
     * {@code $INCLUDE} function for canonical test262 tests
     */
    void include(String file) throws IOException;

    /**
     * {@code runTestCase} function for canonical test262 tests
     */
    void runTestCase(Object testcase);

    /**
     * Additional set-up code after loading {@code sta.js}
     */
    void setUp();
}
