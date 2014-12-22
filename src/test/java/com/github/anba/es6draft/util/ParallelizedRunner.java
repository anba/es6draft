/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.TestWithParameters;

/**
 *
 */
public class ParallelizedRunner extends BlockJUnit4ClassRunnerWithParameters {
    public ParallelizedRunner(TestWithParameters test) throws InitializationError {
        super(test);
    }

    // workaround for: https://github.com/junit-team/junit/issues/1046
    private static final ConcurrentHashMap<Class<?>, TestClass> testClasses = new ConcurrentHashMap<>();

    @Override
    protected TestClass createTestClass(Class<?> clazz) {
        TestClass testClass = testClasses.get(clazz);
        if (testClass == null) {
            testClasses.put(clazz, testClass = new TestClass(clazz));
        }
        return testClass;
    }

    // playing whack-a-mole with new TLAB allocations by re-defining with{Befores,Afters}...

    @Override
    protected Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
        List<FrameworkMethod> list = getTestClass().getAnnotatedMethods(Before.class);
        if (list.isEmpty()) {
            return statement;
        }
        return new BeforesStatement(target, statement, list);
    }

    @Override
    protected Statement withAfters(FrameworkMethod method, Object target, Statement statement) {
        List<FrameworkMethod> list = getTestClass().getAnnotatedMethods(After.class);
        if (list.isEmpty()) {
            return statement;
        }
        return new AftersStatement(target, statement, list);
    }

    private static final class BeforesStatement extends Statement {
        private static final Object[] EMPTY_ARGS = new Object[0];
        private final Object target;
        private final Statement statement;
        private final List<FrameworkMethod> list;

        BeforesStatement(Object target, Statement statement, List<FrameworkMethod> list) {
            this.target = target;
            this.statement = statement;
            this.list = list;
        }

        @Override
        public void evaluate() throws Throwable {
            // (1) Avoid ImmutableCollections#iterator()
            for (int i = 0, size = list.size(); i < size; ++i) {
                list.get(i).invokeExplosively(target, EMPTY_ARGS);
            }
            statement.evaluate();
        }
    }

    private static final class AftersStatement extends Statement {
        private static final Object[] EMPTY_ARGS = new Object[0];
        private final Object target;
        private final Statement statement;
        private final List<FrameworkMethod> list;

        AftersStatement(Object target, Statement statement, List<FrameworkMethod> list) {
            this.target = target;
            this.statement = statement;
            this.list = list;
        }

        @Override
        public void evaluate() throws Throwable {
            // (2) Lazily create ArrayList
            ArrayList<Throwable> throwables = null;
            try {
                statement.evaluate();
            } catch (Throwable e) {
                throwables = new ArrayList<Throwable>();
                throwables.add(e);
            } finally {
                for (int i = 0, size = list.size(); i < size; ++i) {
                    try {
                        list.get(i).invokeExplosively(target, EMPTY_ARGS);
                    } catch (Throwable e) {
                        if (throwables == null) {
                            throwables = new ArrayList<Throwable>();
                        }
                        throwables.add(e);
                    }
                }
            }
            if (throwables != null) {
                MultipleFailureException.assertEmpty(throwables);
            }
        }
    }
}
