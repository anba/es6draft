/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.test262.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.model.Statement;

/**
 * JUnit {@link Suite} implementation similar to {@link Parameterized}. In contrast to the latter
 * one, this implementation gives each test a named label.
 * 
 */
public class LabelledParameterized extends Suite {

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Parallelized {
    }

    /**
     * http://hwellmann.blogspot.de/2009/12/running-parameterized-junit-tests-in.html
     */
    private static class ThreadPoolScheduler implements RunnerScheduler {
        private ExecutorService executor;

        public ThreadPoolScheduler() {
            String threads = System.getProperty("junit.parallel.threads", "8");
            int numThreads = Integer.parseInt(threads);
            executor = Executors.newFixedThreadPool(numThreads);
        }

        @Override
        public void finished() {
            executor.shutdown();
            try {
                executor.awaitTermination(10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void schedule(Runnable childStatement) {
            executor.submit(childStatement);
        }
    }

    private static final class LabelledRunner extends BlockJUnit4ClassRunner {
        private final Object[] params;

        LabelledRunner(Class<?> clazz, Object[] params) throws InitializationError {
            super(clazz);
            this.params = params;
        }

        /**
         * Returns the label which is the first parameter by default
         */
        protected String getLabel() {
            return Objects.toString(params[0]);
        }

        @Override
        protected void validateConstructor(List<Throwable> errors) {
            // remove validation for zero-arg ctor from super-class
            validateOnlyOneConstructor(errors);
        }

        @Override
        protected Statement classBlock(RunNotifier notifier) {
            // override super-class method, dispatch only to children
            return childrenInvoker(notifier);
        }

        @Override
        protected Object createTest() throws Exception {
            // invoke the ctor with the supplied parameters for this test
            Constructor<?> ctor = getTestClass().getOnlyConstructor();
            return ctor.newInstance(params);
        }

        @Override
        protected String getName() {
            return getLabel();
        }

        @Override
        protected String testName(FrameworkMethod method) {
            return method.getName() + "[" + getLabel() + "]";
        }
    }

    private List<Runner> children;

    public LabelledParameterized(Class<?> klass) throws Throwable {
        super(klass, Collections.<Runner> emptyList());
        children = createChildren();
        if (getTestClass().getJavaClass().isAnnotationPresent(Parallelized.class)) {
            setScheduler(new ThreadPoolScheduler());
        }
    }

    @Override
    protected List<Runner> getChildren() {
        return children;
    }

    /**
     * Creates the children {@link Runner} for this runner
     */
    protected List<Runner> createChildren() throws Throwable {
        Class<?> clazz = getTestClass().getJavaClass();
        List<Runner> children = new ArrayList<>();
        List<Object[]> params = retrieveParams();
        for (Object[] p : params) {
            children.add(new LabelledRunner(clazz, p));
        }
        return children;
    }

    /**
     * Retrieves the test parameters as specified in {@link Parameters}
     */
    protected List<Object[]> retrieveParams() throws Throwable {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(Parameters.class);
        final int PUBLIC_STATIC = Modifier.PUBLIC | Modifier.STATIC;
        for (FrameworkMethod fm : methods) {
            Method m = fm.getMethod();
            if ((m.getModifiers() & PUBLIC_STATIC) != 0) {
                return cast(fm.invokeExplosively(null));
            }
        }
        throw new IllegalStateException();
    }

    @SuppressWarnings("unchecked")
    private final <T> T cast(Object o) {
        return (T) o;
    }
}
