/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite for natives tests.
 */
@RunWith(Suite.class)
@SuiteClasses({ NativesMozillaTest.class, NativesV8Test.class })
public final class NativesTestSuite {
}
