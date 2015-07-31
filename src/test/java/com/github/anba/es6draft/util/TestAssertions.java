/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.util.Arrays;

import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.StackTraces;

/**
 * 
 */
public final class TestAssertions {
    private static final boolean FILTER_STACK_TRACE = true;
    private static final String BASE = "com.github.anba.es6draft";
    private static final String[] ALLOWED_PACKAGES = { BASE + ".interpreter", BASE + ".parser", BASE + ".regexp",
            BASE + ".runtime", BASE + ".scripting" };
    private static final String[] HIDDEN_CLASSES = { BASE + ".runtime.internal.RuntimeInfo" };
    private static final String[] HIDDEN_METHODS = { "newInstance" };

    private TestAssertions() {
    }

    /**
     * Creates a new {@link AssertionError} using the given message string.
     * 
     * @param message
     *            the error message
     * @return a new assertion error
     */
    public static AssertionError newAssertionError(String message) {
        AssertionError error = new AssertionError(message);
        error.setStackTrace(StackTraces.scriptStackTrace(error.getStackTrace()));
        return error;
    }

    /**
     * Creates a new {@link AssertionError} for the parser exception.
     * 
     * @param e
     *            the script exception
     * @return a new assertion error
     */
    public static AssertionError toAssertionError(ParserException e) {
        ParserException exception = withFilteredStackTrace(e);
        AssertionError error = new AssertionError(exception.getMessage(), exception);
        error.setStackTrace(StackTraces.scriptStackTrace(exception));
        return error;
    }

    /**
     * Creates a new {@link AssertionError} for the script exception.
     * 
     * @param cx
     *            the execution context
     * @param e
     *            the script exception
     * @return a new assertion error
     */
    public static AssertionError toAssertionError(ExecutionContext cx, ScriptException e) {
        ScriptException exception = withFilteredStackTrace(e);
        AssertionError error = new AssertionError(exception.getMessage(cx), exception);
        error.setStackTrace(exception.getScriptStackTrace());
        return error;
    }

    private static ParserException withFilteredStackTrace(ParserException e) {
        if (!FILTER_STACK_TRACE) {
            return e;
        }
        ParserException exception = new ParserException(e.getType(), e.getFile(), e.getLine(), e.getColumn(),
                e.getMessageKey(), e.getMessageArguments());
        exception.setStackTrace(filterStackTrace(e));
        return exception;
    }

    private static ScriptException withFilteredStackTrace(ScriptException e) {
        if (!FILTER_STACK_TRACE) {
            return e;
        }
        ScriptException exception = new ScriptException(e.getValue(), e.getCause());
        exception.setStackTrace(filterStackTrace(e));
        return exception;
    }

    private static StackTraceElement[] filterStackTrace(Throwable e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        StackTraceElement[] filtered = new StackTraceElement[stackTrace.length];
        int top = 0;
        for (int i = 0; i < stackTrace.length; ++i) {
            StackTraceElement frame = stackTrace[i];
            if (isVisibleFrame(frame)) {
                filtered[top++] = frame;
            }
        }
        return Arrays.copyOf(filtered, top);
    }

    private static boolean isVisibleFrame(StackTraceElement frame) {
        String className = frame.getClassName();
        if (className.startsWith("#")) {
            // Always display script frames.
            return true;
        }
        PACKAGE: {
            for (String packageName : ALLOWED_PACKAGES) {
                if (className.startsWith(packageName)) {
                    break PACKAGE;
                }
            }
            return false;
        }
        for (String hiddenClass : HIDDEN_CLASSES) {
            if (className.startsWith(hiddenClass)) {
                return false;
            }
        }
        String methodName = frame.getMethodName();
        for (String hiddenMethod : HIDDEN_METHODS) {
            if (hiddenMethod.equals(methodName)) {
                return false;
            }
        }
        return true;
    }
}
