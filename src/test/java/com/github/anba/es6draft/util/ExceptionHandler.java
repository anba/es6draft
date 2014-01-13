/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;

import org.hamcrest.Matcher;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Exception handler like {@link TestRule}
 * 
 */
public abstract class ExceptionHandler implements TestRule {
    private Matcher<?> matcher;

    public ExceptionHandler() {
        this(nothing());
    }

    public ExceptionHandler(Matcher<?> matcher) {
        match(matcher);
    }

    public void match(Matcher<?> matcher) {
        this.matcher = requireNonNull(matcher);
    }

    protected static final Matcher<?> nothing() {
        return not(anything());
    }

    /**
     * To be implemented by subclasses
     */
    protected abstract void handle(Throwable t);

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } catch (Throwable t) {
                    if (matcher.matches(t)) {
                        handle(t);
                    } else {
                        throw t;
                    }
                }
            }
        };
    }
}
