/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;

/**
 * {@link Matcher} for script execution error messages
 */
public class ErrorMessageMatcher<T extends Throwable> extends TypeSafeMatcher<T> {
    private final ExecutionContext cx;
    private final Matcher<String> matcher;

    public ErrorMessageMatcher(ExecutionContext cx, Matcher<String> matcher) {
        super(Throwable.class);
        this.cx = cx;
        this.matcher = matcher;
    }

    public static <T extends Throwable> Matcher<T> hasErrorMessage(ExecutionContext cx,
            Matcher<String> matcher) {
        return new ErrorMessageMatcher<>(cx, matcher);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("exception with error-message ").appendDescriptionOf(matcher);
    }

    @Override
    protected void describeMismatchSafely(T item, Description mismatchDescription) {
        mismatchDescription.appendText("error-message ");
        matcher.describeMismatch(getMessage(item), mismatchDescription);
    }

    @Override
    public boolean matchesSafely(T error) {
        return matcher.matches(getMessage(error));
    }

    protected String getMessage(T error) {
        if (error instanceof ScriptException) {
            return ((ScriptException) error).getMessage(cx);
        }
        if (error instanceof StackOverflowError) {
            return String.format("InternalError: %s",
                    cx.getRealm().message(Messages.Key.StackOverflow));
        }
        return error.getMessage();
    }
}
