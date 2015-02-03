/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.objects.ErrorObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;

/**
 * Static helper methods to create {@link ScriptException} objects.
 */
public final class Errors {
    private Errors() {
    }

    private static ScriptException newError(ExecutionContext cx, Intrinsics prototype,
            String message) {
        return new ErrorObject(cx.getRealm(), prototype, message).getException();
    }

    private static ScriptException newError(ExecutionContext cx, Intrinsics prototype,
            String message, String file, int line, int column) {
        return new ErrorObject(cx.getRealm(), prototype, message, file, line, column)
                .getException();
    }

    private static ScriptException newError(ExecutionContext cx, Intrinsics prototype,
            Messages.Key key) {
        String message = cx.getRealm().message(key);
        return new ErrorObject(cx.getRealm(), prototype, message).getException();
    }

    private static ScriptException newError(ExecutionContext cx, Intrinsics prototype,
            Messages.Key key, String... args) {
        String message = cx.getRealm().message(key, args);
        return new ErrorObject(cx.getRealm(), prototype, message).getException();
    }

    /**
     * Returns a new {@code Error} instance.
     * 
     * @param cx
     *            the execution context
     * @param message
     *            the error message
     * @return the new script exception object
     */
    public static ScriptException newError(ExecutionContext cx, String message) {
        return newError(cx, Intrinsics.ErrorPrototype, message);
    }

    /**
     * Returns a new {@code InternalError} instance.
     * 
     * @param cx
     *            the execution context
     * @param key
     *            the error message key
     * @return the new script exception object
     */
    public static ScriptException newInternalError(ExecutionContext cx, Messages.Key key) {
        return newError(cx, Intrinsics.InternalErrorPrototype, key);
    }

    /**
     * Returns a new {@code InternalError} instance.
     * 
     * @param cx
     *            the execution context
     * @param key
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the new script exception object
     */
    public static ScriptException newInternalError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return newError(cx, Intrinsics.InternalErrorPrototype, key, args);
    }

    /**
     * Returns a new {@code TypeError} instance.
     * 
     * @param cx
     *            the execution context
     * @param key
     *            the error message key
     * @return the new script exception object
     */
    public static ScriptException newTypeError(ExecutionContext cx, Messages.Key key) {
        return newError(cx, Intrinsics.TypeErrorPrototype, key);
    }

    /**
     * Returns a new {@code TypeError} instance.
     * 
     * @param cx
     *            the execution context
     * @param key
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the new script exception object
     */
    public static ScriptException newTypeError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return newError(cx, Intrinsics.TypeErrorPrototype, key, args);
    }

    /**
     * Returns a new {@code ReferenceError} instance.
     * 
     * @param cx
     *            the execution context
     * @param key
     *            the error message key
     * @return the new script exception object
     */
    public static ScriptException newReferenceError(ExecutionContext cx, Messages.Key key) {
        return newError(cx, Intrinsics.ReferenceErrorPrototype, key);
    }

    /**
     * Returns a new {@code ReferenceError} instance.
     * 
     * @param cx
     *            the execution context
     * @param key
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the new script exception object
     */
    public static ScriptException newReferenceError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return newError(cx, Intrinsics.ReferenceErrorPrototype, key, args);
    }

    /**
     * Returns a new {@code ReferenceError} instance.
     * 
     * @param cx
     *            the execution context
     * @param message
     *            the error message
     * @param file
     *            the file name
     * @param line
     *            the line number
     * @param column
     *            the column number
     * @return the new script exception object
     */
    public static ScriptException newReferenceError(ExecutionContext cx, String message,
            String file, int line, int column) {
        return newError(cx, Intrinsics.ReferenceErrorPrototype, message, file, line, column);
    }

    /**
     * Returns a new {@code SyntaxError} instance.
     * 
     * @param cx
     *            the execution context
     * @param key
     *            the error message key
     * @return the new script exception object
     */
    public static ScriptException newSyntaxError(ExecutionContext cx, Messages.Key key) {
        return newError(cx, Intrinsics.SyntaxErrorPrototype, key);
    }

    /**
     * Returns a new {@code SyntaxError} instance.
     * 
     * @param cx
     *            the execution context
     * @param key
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the new script exception object
     */
    public static ScriptException newSyntaxError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return newError(cx, Intrinsics.SyntaxErrorPrototype, key, args);
    }

    /**
     * Returns a new {@code SyntaxError} instance.
     * 
     * @param cx
     *            the execution context
     * @param message
     *            the error message
     * @param file
     *            the file name
     * @param line
     *            the line number
     * @param column
     *            the column number
     * @return the new script exception object
     */
    public static ScriptException newSyntaxError(ExecutionContext cx, String message, String file,
            int line, int column) {
        return newError(cx, Intrinsics.SyntaxErrorPrototype, message, file, line, column);
    }

    /**
     * Returns a new {@code RangeError} instance.
     * 
     * @param cx
     *            the execution context
     * @param key
     *            the error message key
     * @return the new script exception object
     */
    public static ScriptException newRangeError(ExecutionContext cx, Messages.Key key) {
        return newError(cx, Intrinsics.RangeErrorPrototype, key);
    }

    /**
     * Returns a new {@code RangeError} instance.
     * 
     * @param cx
     *            the execution context
     * @param key
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the new script exception object
     */
    public static ScriptException newRangeError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return newError(cx, Intrinsics.RangeErrorPrototype, key, args);
    }

    /**
     * Returns a new {@code URIError} instance.
     * 
     * @param cx
     *            the execution context
     * @param key
     *            the error message key
     * @return the new script exception object
     */
    public static ScriptException newURIError(ExecutionContext cx, Messages.Key key) {
        return newError(cx, Intrinsics.URIErrorPrototype, key);
    }

    /**
     * Returns a new {@code URIError} instance.
     * 
     * @param cx
     *            the execution context
     * @param key
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the new script exception object
     */
    public static ScriptException newURIError(ExecutionContext cx, Messages.Key key, String... args) {
        return newError(cx, Intrinsics.URIErrorPrototype, key, args);
    }
}
