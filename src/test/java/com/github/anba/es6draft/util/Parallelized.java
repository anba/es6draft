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
    private static class ThreadPoolScheduler implements RunnerScheduler {
        private ExecutorService executor;

        public ThreadPoolScheduler() {
            int numThreads = Runtime.getRuntime().availableProcessors() * 2;
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

    public Parallelized(Class<?> klass) throws Throwable {
        super(klass);
        setScheduler(new ThreadPoolScheduler());
    }
}
