/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

/**
 *
 */
public class ParallelizedRunnerFactory implements ParametersRunnerFactory {
    @Override
    public Runner createRunnerForTestWithParameters(TestWithParameters test)
            throws InitializationError {
        return new ParallelizedRunner(test);
    }
}
