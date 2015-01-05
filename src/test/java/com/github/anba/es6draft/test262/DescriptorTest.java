/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import com.github.anba.es6draft.util.Functional.BiFunction;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParallelizedRunnerFactory;
import com.github.anba.es6draft.util.Resources;

/**
 * Basic test to ensure test file descriptors can be parsed.
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParallelizedRunnerFactory.class)
public class DescriptorTest {
    private static final Configuration configuration = loadConfiguration(Test262Web.class);

    @Parameters(name = "{0}")
    public static List<Test262Info> suiteValues() throws IOException {
        return Resources.loadTests(configuration, new BiFunction<Path, Path, Test262Info>() {
            @Override
            public Test262Info apply(Path basedir, Path file) {
                return new Test262Info(basedir, file);
            }
        });
    }

    @Parameter(0)
    public Test262Info test;

    @Test
    public void test() throws IOException {
        // Test readFileInformation() does not throw any exceptions
        String sourceCode = new String(Files.readAllBytes(test.toFile()), StandardCharsets.UTF_8);
        test.readFileInformation(sourceCode);
    }
}
