/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.junit.runners.model.RunnerScheduler;

/**
 * JUnit {@link Suite} implementation similar to {@link Parameterized} with support for multiple
 * threads
 * 
 * @see <a href="http://hwellmann.blogspot.de/2009/12/running-parameterized-junit-tests-in.html">
 *      http://hwellmann.blogspot.de/2009/12/running-parameterized-junit-tests-in.html</a>
 */
public class Parallelized extends Parameterized {
    private static final class ThreadPoolScheduler implements RunnerScheduler {
        private final ExecutorService executor;

        public ThreadPoolScheduler(ExecutorService executor) {
            this.executor = executor;
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

    public Parallelized(Class<?> klass) throws Throwable {
        super(klass);
        setScheduler(new ThreadPoolScheduler(createExecutor(klass)));
    }

    protected ExecutorService createExecutor(Class<?> klass) {
        int numThreads = numberOfThreads(klass);
        return Executors.newFixedThreadPool(numThreads);
    }

    protected int numberOfThreads(Class<?> klass) {
        Concurrency concurrency = klass.getAnnotation(Concurrency.class);
        int threads;
        float factor;
        if (concurrency != null) {
            threads = concurrency.threads();
            factor = concurrency.factor();
        } else {
            threads = getDefaultValue(Concurrency.class, "threads", Integer.class);
            factor = getDefaultValue(Concurrency.class, "factor", Float.class);
        }
        threads = threads > 0 ? threads : Runtime.getRuntime().availableProcessors();
        factor = Math.max(factor, 0);
        return Math.max((int) Math.round(threads * factor), 1);
    }

    private static <T> T getDefaultValue(Class<?> annotation, String methodName, Class<T> valueType) {
        assert annotation.isAnnotation();
        try {
            return valueType.cast(annotation.getMethod(methodName).getDefaultValue());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
