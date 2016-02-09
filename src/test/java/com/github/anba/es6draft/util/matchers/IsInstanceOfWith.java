/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util.matchers;

import java.util.Arrays;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsInstanceOf;

/**
 * Combined {@link IsInstanceOf} with an additional predicate {@link Matcher}.
 */
public class IsInstanceOfWith<T> extends DiagnosingMatcher<Object> {
    private final IsInstanceOf instanceOf;
    private final Matcher<T> predicate;

    public IsInstanceOfWith(Class<? extends T> expectedClass, Matcher<T> predicate) {
        this.instanceOf = new IsInstanceOf(expectedClass);
        this.predicate = predicate;
    }

    @Override
    public void describeTo(Description description) {
        description.appendList("(", " " + "and" + " ", ")", Arrays.asList(instanceOf, predicate));
    }

    @Override
    protected boolean matches(Object item, Description mismatchDescription) {
        if (!instanceOf.matches(item)) {
            instanceOf.describeMismatch(item, mismatchDescription);
            return false;
        }
        if (!predicate.matches(item)) {
            predicate.describeMismatch(item, mismatchDescription);
            return false;
        }
        return true;
    }

    /**
     * Factory method to create a new matcher for this class.
     */
    public static <T> IsInstanceOfWith<T> instanceOfWith(Class<? extends T> expectedClass, Matcher<T> predicate) {
        return new IsInstanceOfWith<>(expectedClass, predicate);
    }
}
