/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.internal.ScriptRuntime._throw;

import java.text.MessageFormat;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Static helper methods to create and throw {@link ScriptException} objects
 */
public final class Errors {
    private Errors() {
    }

    private static Object newError(ExecutionContext cx, Intrinsics constructor, Messages.Key key) {
        Realm realm = cx.getRealm();
        String message = realm.message(key);
        ScriptObject nativeError = realm.getIntrinsic(constructor);
        return ((Constructor) nativeError).construct(cx, message);
    }

    private static Object newError(ExecutionContext cx, Intrinsics constructor, Messages.Key key,
            String... args) {
        Realm realm = cx.getRealm();
        MessageFormat format = new MessageFormat(realm.message(key), realm.getLocale());
        String message = format.format(args);
        ScriptObject nativeError = realm.getIntrinsic(constructor);
        return ((Constructor) nativeError).construct(cx, message);
    }

    /**
     * Throws a new {@code InternalError} instance
     */
    public static ScriptException throwInternalError(ExecutionContext cx, Messages.Key key) {
        return _throw(newError(cx, Intrinsics.InternalError, key));
    }

    /**
     * Throws a new {@code InternalError} instance
     */
    public static ScriptException throwInternalError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return _throw(newError(cx, Intrinsics.InternalError, key, args));
    }

    /**
     * Throws a new {@code TypeError} instance
     */
    public static ScriptException throwTypeError(ExecutionContext cx, Messages.Key key) {
        return _throw(newError(cx, Intrinsics.TypeError, key));
    }

    /**
     * Throws a new {@code TypeError} instance
     */
    public static ScriptException throwTypeError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return _throw(newError(cx, Intrinsics.TypeError, key, args));
    }

    /**
     * Throws a new {@code ReferenceError} instance
     */
    public static ScriptException throwReferenceError(ExecutionContext cx, Messages.Key key) {
        return _throw(newError(cx, Intrinsics.ReferenceError, key));
    }

    /**
     * Throws a new {@code ReferenceError} instance
     */
    public static ScriptException throwReferenceError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return _throw(newError(cx, Intrinsics.ReferenceError, key, args));
    }

    /**
     * Throws a new {@code SyntaxError} instance
     */
    public static ScriptException throwSyntaxError(ExecutionContext cx, Messages.Key key) {
        return _throw(newError(cx, Intrinsics.SyntaxError, key));
    }

    /**
     * Throws a new {@code SyntaxError} instance
     */
    public static ScriptException throwSyntaxError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return _throw(newError(cx, Intrinsics.SyntaxError, key, args));
    }

    /**
     * Throws a new {@code RangeError} instance
     */
    public static ScriptException throwRangeError(ExecutionContext cx, Messages.Key key) {
        return _throw(newError(cx, Intrinsics.RangeError, key));
    }

    /**
     * Throws a new {@code RangeError} instance
     */
    public static ScriptException throwRangeError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return _throw(newError(cx, Intrinsics.RangeError, key, args));
    }

    /**
     * Throws a new {@code URIError} instance
     */
    public static ScriptException throwURIError(ExecutionContext cx, Messages.Key key) {
        return _throw(newError(cx, Intrinsics.URIError, key));
    }

    /**
     * Throws a new {@code URIError} instance
     */
    public static ScriptException throwURIError(ExecutionContext cx, Messages.Key key,
            String... args) {
        return _throw(newError(cx, Intrinsics.URIError, key, args));
    }
}
