/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.configuration.Configuration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.Resources;

/**
 * Basic test to ensure test file descriptors can be parsed.
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParameterizedRunnerFactory.class)
public final class DescriptorTest {
    private static final Configuration configuration = loadConfiguration(Test262Web.class);

    private static Set<String> features;

    @BeforeClass
    public static void setUpClass() throws IOException {
        features = Files.lines(Resources.getTestSuitePath(configuration).resolve("features.txt"))
                .filter(line -> !line.startsWith("#")).collect(Collectors.toSet());
    }

    @Parameters(name = "{0}")
    public static List<Test262Info> suiteValues() throws IOException {
        return Resources.loadTests(configuration, Test262Info::new);
    }

    @Parameter(0)
    public Test262Info test;

    @Test
    public void test() throws IOException {
        try {
            test.readFileStrict();
        } catch (Test262Info.MalformedDataException e) {
            fail(e.getMessage());
        }
        assertTrue(features.containsAll(test.getFeatures()));
    }
}
