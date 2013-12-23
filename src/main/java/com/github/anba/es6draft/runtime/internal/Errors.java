/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.objects.ErrorObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;

/**
 * Static helper methods to create {@link ScriptException} objects
 */
public final class Errors {
    private Errors() {
    }

    private static ScriptException newError(ExecutionContext cx, Intrinsics constructor,
            String message) {
        Callable nativeError = (Callable) cx.getIntrinsic(constructor);
        return ((ErrorObject) nativeError.call(cx, UNDEFINED, message)).getException();
    }

    private static ScriptException newError(ExecutionContext cx, Intrinsics constructor,
            String message, String file, int line, int column) {
        Callable nativeError = (Callable) cx.getIntrinsic(constructor);
        return ((ErrorObject) nativeError.call(cx, UNDEFINED, message, file, line, column))
                .getException();
    }

    private static ScriptException newError(ExecutionContext cx, Intrinsics constructor,
            Messages.Key key) {
        return newError(cx, constructor, cx.getRealm().message(key));
    }

    private static ScriptException newError(ExecutionContext cx, Intrinsics constructor,
            Messages.Key key, String... args) {
        return newError(cx, constructor, cx.getRealm().message(key, args));
    }

    /**
     * Returns a new {@code Error} instance
     */
    public static ScriptException newError(ExecutionContext cx, String message) {
        return newError(cx, Intrinsics.Error, message);
    }

    /**
     * Returns a new {@code InternalError} instance
     */
    public static ScriptException newInternalError(ExecutionContext cx, Messages.Key key) {
        return newError(cx, Intrinsics.InternalError, key);
    }

    /**
     * Returns a new {@code InternalError} instance
     */
    public static ScriptException newInternalError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return newError(cx, Intrinsics.InternalError, key, args);
    }

    /**
     * Returns a new {@code TypeError} instance
     */
    public static ScriptException newTypeError(ExecutionContext cx, Messages.Key key) {
        return newError(cx, Intrinsics.TypeError, key);
    }

    /**
     * Returns a new {@code TypeError} instance
     */
    public static ScriptException newTypeError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return newError(cx, Intrinsics.TypeError, key, args);
    }

    /**
     * Returns a new {@code ReferenceError} instance
     */
    public static ScriptException newReferenceError(ExecutionContext cx, Messages.Key key) {
        return newError(cx, Intrinsics.ReferenceError, key);
    }

    /**
     * Returns a new {@code ReferenceError} instance
     */
    public static ScriptException newReferenceError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return newError(cx, Intrinsics.ReferenceError, key, args);
    }

    /**
     * Returns a new {@code ReferenceError} instance
     */
    public static ScriptException newReferenceError(ExecutionContext cx, String message,
            String file, int line, int column) {
        return newError(cx, Intrinsics.ReferenceError, message, file, line, column);
    }

    /**
     * Returns a new {@code SyntaxError} instance
     */
    public static ScriptException newSyntaxError(ExecutionContext cx, Messages.Key key) {
        return newError(cx, Intrinsics.SyntaxError, key);
    }

    /**
     * Returns a new {@code SyntaxError} instance
     */
    public static ScriptException newSyntaxError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return newError(cx, Intrinsics.SyntaxError, key, args);
    }

    /**
     * Returns a new {@code SyntaxError} instance
     */
    public static ScriptException newSyntaxError(ExecutionContext cx, String message, String file,
            int line, int column) {
        return newError(cx, Intrinsics.SyntaxError, message, file, line, column);
    }

    /**
     * Returns a new {@code RangeError} instance
     */
    public static ScriptException newRangeError(ExecutionContext cx, Messages.Key key) {
        return newError(cx, Intrinsics.RangeError, key);
    }

    /**
     * Returns a new {@code RangeError} instance
     */
    public static ScriptException newRangeError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return newError(cx, Intrinsics.RangeError, key, args);
    }

    /**
     * Returns a new {@code URIError} instance
     */
    public static ScriptException newURIError(ExecutionContext cx, Messages.Key key) {
        return newError(cx, Intrinsics.URIError, key);
    }

    /**
     * Returns a new {@code URIError} instance
     */
    public static ScriptException newURIError(ExecutionContext cx, Messages.Key key, String... args) {
        return newError(cx, Intrinsics.URIError, key, args);
    }
}
