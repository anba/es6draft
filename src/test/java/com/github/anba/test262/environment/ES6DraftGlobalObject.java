/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.test262.environment;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.types.Callable;

/**
 * Shared global object definition for all test suites. Includes all necessary global function
 * definitions to run sputnik, ietestcenter and canonical test262 test cases.
 */
public abstract class ES6DraftGlobalObject extends
        com.github.anba.es6draft.runtime.objects.GlobalObject implements GlobalObject {
    public ES6DraftGlobalObject(Realm realm) {
        super(realm);
    }

    /**
     * Parses and executes the given file
     */
    protected abstract void include(Path file) throws IOException;

    /**
     * Process test failure with message
     */
    protected abstract void failure(String message);

    /**
     * Returns {@code true} iff strict-mode semantics are supported
     */
    protected abstract boolean isStrictSupported();

    /**
     * Returns the current test description
     */
    protected abstract String getDescription();

    @Override
    @Function(name = "$ERROR", arity = 1, attributes = @Attributes(writable = false,
            enumerable = true, configurable = false))
    public void error(String message) {
        failure(message);
    }

    @Override
    @Function(name = "$FAIL", arity = 1, attributes = @Attributes(writable = false,
            enumerable = true, configurable = false))
    public void fail(String message) {
        failure(message);
    }

    @Override
    @Function(name = "$PRINT", arity = 1, attributes = @Attributes(writable = false,
            enumerable = true, configurable = false))
    public void print(String message) {
        System.out.println(message);
    }

    @Override
    @Function(name = "$INCLUDE", arity = 1, attributes = @Attributes(writable = false,
            enumerable = true, configurable = false))
    public void include(String file) throws IOException {
        include(Paths.get(file));
    }

    @Override
    @Function(name = "runTestCase", arity = 1, attributes = @Attributes(writable = false,
            enumerable = true, configurable = false))
    public void runTestCase(Object testcase) {
        Callable fn = (Callable) testcase;
        Object value = fn.call(UNDEFINED);
        if (!ToBoolean(value)) {
            failure(getDescription());
        }
    }
}
