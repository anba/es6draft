/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.moztest;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.anba.es6draft.util.Parallelized;

/**
 *
 */
@Ignore
@RunWith(Parallelized.class)
public class MozillaJitTest extends BaseMozillaTest {

    /**
     * Returns a {@link Path} which points to the test directory 'mozilla.js.tests'
     */
    private static Path testDir() {
        String testPath = System.getenv("MOZ_JITTESTS");
        return (testPath != null ? Paths.get(testPath) : null);
    }

    @Parameters
    public static Iterable<Object[]> mozillaSuiteValues() throws IOException {
        Path testdir = testDir();
        assumeThat("missing system property 'MOZ_JITTESTS'", testdir, notNullValue());
        assumeTrue("directy 'MOZ_JITTESTS' does not exist", Files.exists(testdir));
        return toObjectArray(Collections.<MozTest> emptyList());
    }

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(10));

    @Parameter(0)
    public MozTest moztest;
}
