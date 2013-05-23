/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.util.Functional.iterable;
import static com.github.anba.es6draft.util.Functional.map;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.instanceOf;

import java.util.regex.Pattern;

import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.ErrorObject;
import com.github.anba.es6draft.util.Functional.Function;

/**
 * Base class for {@link Test262} and {@link Test262Strict}
 */
abstract class BaseTest262 {

    /**
     * anyOf(asList(types).map(x -> instanceOf(x)))
     */
    protected static final Matcher<Object> anyInstanceOf(final Class<?>... types) {
        return anyOf(map(iterable(types), new Function<Class<?>, Matcher<? super Object>>() {
            @Override
            public Matcher<? super Object> apply(Class<?> type) {
                return instanceOf(type);
            }
        }));
    }

    protected static final Class<?>[] exceptions() {
        return new Class[] { ScriptException.class, ParserException.class };
    }

    protected static final <T extends Throwable> Matcher<T> hasErrorType(final String errorType) {
        return new TypeSafeMatcher<T>(RuntimeException.class) {
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText(String.format("exception with error-type '%s'", errorType));
            }

            @Override
            public boolean matchesSafely(T error) {
                // errorType is now a regular expression
                Pattern p = Pattern.compile(errorType, Pattern.CASE_INSENSITIVE);
                String name;
                if (error instanceof ScriptException) {
                    Object value = ((ScriptException) error).getValue();
                    if (value instanceof ErrorObject) {
                        name = value.toString();
                    } else {
                        name = "";
                    }
                } else if (error instanceof ParserException) {
                    name = ((ParserException) error).getFormattedMessage();
                } else {
                    name = "";
                }
                return p.matcher(name).find();
            }
        };
    }
}
