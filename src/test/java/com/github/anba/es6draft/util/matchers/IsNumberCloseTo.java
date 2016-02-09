/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.number.IsCloseTo;

/**
 * {@link IsCloseTo} like matcher for {@link Number} instances.
 */
public class IsNumberCloseTo extends TypeSafeMatcher<Number> {
    private final IsCloseTo closeTo;

    public IsNumberCloseTo(double value, double error) {
        this.closeTo = new IsCloseTo(value, error);
    }

    @Override
    public boolean matchesSafely(Number item) {
        return closeTo.matchesSafely(item.doubleValue());
    }

    @Override
    public void describeMismatchSafely(Number item, Description mismatchDescription) {
        closeTo.describeMismatchSafely(item.doubleValue(), mismatchDescription);
    }

    @Override
    public void describeTo(Description description) {
        closeTo.describeTo(description);
    }

    /**
     * Factory method for {@link IsNumberCloseTo}.
     */
    public static Matcher<Number> numberCloseTo(double operand, double error) {
        return new IsNumberCloseTo(operand, error);
    }

    /**
     * Short cut for {@code numberCloseTo(operand, Math.ulp(1.0))}.
     */
    public static Matcher<Number> numberCloseTo(double operand) {
        return numberCloseTo(operand, Math.ulp(1.0));
    }
}
