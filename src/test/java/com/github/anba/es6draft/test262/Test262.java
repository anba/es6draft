/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.test262.Resources.loadConfiguration;
import static com.github.anba.es6draft.test262.Resources.loadTestCases;
import static com.github.anba.es6draft.util.Functional.iterable;
import static com.github.anba.es6draft.util.Functional.map;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.instanceOf;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.ErrorObject;
import com.github.anba.es6draft.util.ExceptionHandler;
import com.github.anba.es6draft.util.Functional.Function;
import com.github.anba.es6draft.util.LazyInit;
import com.github.anba.es6draft.util.Parallelized;

/**
 * The standard test262 test suite
 * 
 */
@RunWith(Parallelized.class)
public final class Test262 {
    private static final String TEST_SUITE = "test.suite.test262";

    private static Set<CompatibilityOption> options = CompatibilityOption.WebCompatibility();
    private static final ScriptCache cache = new ScriptCache(Parser.Option.from(options));

    private static final LazyInit<Configuration> configuration = new LazyInit<Configuration>() {
        @Override
        protected Configuration initialize() {
            return loadConfiguration("resource:test262.properties").subset(TEST_SUITE);
        }
    };

    @Parameter(0)
    public String sourceName;

    @Parameter(1)
    public String path;

    @Rule
    public ExpectedException expected = ExpectedException.none().handleAssertionErrors();

    @Rule
    public ExceptionHandler handler = new ExceptionHandler() {
        @Override
        protected void handle(Throwable t) {
            throw new AssertionError(t.getMessage(), t);
        }
    };

    @Test
    public void test() throws Throwable {
        final Path libpath = Paths.get(configuration.get().getString("lib_path"));
        Path testfile = Paths.get(path);
        final Test262Info info = Test262Info.from(testfile);

        Realm realm = Realm.newRealm(new Realm.GlobalObjectCreator<Test262GlobalObject>() {
            @Override
            public Test262GlobalObject createGlobal(Realm realm) {
                return new Test262GlobalObject(realm, libpath, cache, info, sourceName);
            }
        }, options);

        // start initialization
        ExecutionContext cx = realm.defaultContext();
        Test262GlobalObject global = (Test262GlobalObject) realm.getGlobalThis();
        global.include("sta.js");
        createProperties(global, cx, Test262GlobalObject.class);

        Matcher<Object> m = anyInstanceOf(exceptions());
        if (info.isNegative()) {
            m = either(m).or(instanceOf(Test262AssertionError.class));
            expected.expect(m);
            String errorType = info.getErrorType();
            if (errorType != null) {
                expected.expect(hasErrorType(errorType));
            }
        } else {
            handler.match(m);
        }

        global.eval(testfile);
    }

    @Parameters(name = "{0}")
    public static List<Object[]> files() throws IOException {
        List<Object[]> testCases = loadTestCases(configuration.get());
        if (testCases.isEmpty()) {
            System.err.println("no testcases selected!");
        }
        return testCases;
    }

    /**
     * anyOf(asList(types).map(x -> instanceOf(x)))
     */
    private static final Matcher<Object> anyInstanceOf(final Class<?>... types) {
        return anyOf(map(iterable(types), new Function<Class<?>, Matcher<? super Object>>() {
            @Override
            public Matcher<? super Object> apply(Class<?> type) {
                return instanceOf(type);
            }
        }));
    }

    private Class<?>[] exceptions() {
        return new Class[] { ScriptException.class, ParserException.class };
    }

    private static final <T extends Throwable> Matcher<T> hasErrorType(final String errorType) {
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
