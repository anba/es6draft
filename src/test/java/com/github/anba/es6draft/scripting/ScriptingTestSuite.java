/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite for JSR-223 Scripting API tests.
 */
@RunWith(Suite.class)
@SuiteClasses({ ScriptEngineFactoryTest.class, ScriptEngineTest.class, ScriptEngineScopeTest.class,
        TypeConversionTest.class, InvocableTest.class, CompilableTest.class })
public final class ScriptingTestSuite {
}
