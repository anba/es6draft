/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util.matchers;

import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * {@link Matcher} to match strings against regular expressions
 */
public class PatternMatcher extends TypeSafeMatcher<String> {
    private final String regex;
    private final int flags;

    public PatternMatcher(String regex, int flags) {
        this.regex = regex;
        this.flags = flags;
    }

    public static Matcher<String> matchesPattern(String regex) {
        return new PatternMatcher(regex, 0);
    }

    public static Matcher<String> matchesPattern(String regex, int flags) {
        return new PatternMatcher(regex, flags);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("string matching '%s'", regex));
    }

    @Override
    protected void describeMismatchSafely(String item, Description mismatchDescription) {
        mismatchDescription.appendText("was ").appendValue(item);
    }

    @Override
    protected boolean matchesSafely(String item) {
        return getPattern().matcher(item).find();
    }

    private Pattern getPattern() {
        return Pattern.compile(regex, flags);
    }
}
