/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.internal.ScriptRuntime._throw;

import java.text.MessageFormat;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.objects.NativeError;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Scriptable;

/**
 *
 */
public final class Errors {
    private Errors() {
    }

    private static String format(Realm realm, Messages.Key key, String... args) {
        String pattern = realm.message(key);
        MessageFormat format = new MessageFormat(pattern, realm.getLocale());
        return format.format(args);
    }

    public static ScriptException throwInternalError(Realm realm, Messages.Key key) {
        String message = realm.message(key);
        Scriptable nativeError = realm.getNativeError(NativeError.ErrorType.InternalError);
        Object error = ((Constructor) nativeError).construct(message);
        return _throw(error);
    }

    public static ScriptException throwInternalError(Realm realm, Messages.Key key, String... args) {
        String message = format(realm, key, args);
        Scriptable nativeError = realm.getNativeError(NativeError.ErrorType.InternalError);
        Object error = ((Constructor) nativeError).construct(message);
        return _throw(error);
    }

    public static ScriptException throwTypeError(Realm realm, Messages.Key key) {
        String message = realm.message(key);
        Scriptable nativeError = realm.getNativeError(NativeError.ErrorType.TypeError);
        Object error = ((Constructor) nativeError).construct(message);
        return _throw(error);
    }

    public static ScriptException throwTypeError(Realm realm, Messages.Key key, String... args) {
        String message = format(realm, key, args);
        Scriptable nativeError = realm.getNativeError(NativeError.ErrorType.TypeError);
        Object error = ((Constructor) nativeError).construct(message);
        return _throw(error);
    }

    public static ScriptException throwReferenceError(Realm realm, Messages.Key key) {
        String message = realm.message(key);
        Scriptable nativeError = realm.getNativeError(NativeError.ErrorType.ReferenceError);
        Object error = ((Constructor) nativeError).construct(message);
        return _throw(error);
    }

    public static ScriptException throwReferenceError(Realm realm, Messages.Key key, String... args) {
        String message = format(realm, key, args);
        Scriptable nativeError = realm.getNativeError(NativeError.ErrorType.ReferenceError);
        Object error = ((Constructor) nativeError).construct(message);
        return _throw(error);
    }

    public static ScriptException throwSyntaxError(Realm realm, Messages.Key key) {
        String message = realm.message(key);
        Scriptable nativeError = realm.getNativeError(NativeError.ErrorType.SyntaxError);
        Object error = ((Constructor) nativeError).construct(message);
        return _throw(error);
    }

    public static ScriptException throwSyntaxError(Realm realm, Messages.Key key, String... args) {
        String message = format(realm, key, args);
        Scriptable nativeError = realm.getNativeError(NativeError.ErrorType.SyntaxError);
        Object error = ((Constructor) nativeError).construct(message);
        return _throw(error);
    }

    public static ScriptException throwRangeError(Realm realm, Messages.Key key) {
        String message = realm.message(key);
        Scriptable nativeError = realm.getNativeError(NativeError.ErrorType.RangeError);
        Object error = ((Constructor) nativeError).construct(message);
        return _throw(error);
    }

    public static ScriptException throwRangeError(Realm realm, Messages.Key key, String... args) {
        String message = format(realm, key, args);
        Scriptable nativeError = realm.getNativeError(NativeError.ErrorType.RangeError);
        Object error = ((Constructor) nativeError).construct(message);
        return _throw(error);
    }

    public static ScriptException throwURIError(Realm realm, Messages.Key key) {
        String message = realm.message(key);
        Scriptable nativeError = realm.getNativeError(NativeError.ErrorType.URIError);
        Object error = ((Constructor) nativeError).construct(message);
        return _throw(error);
    }

    public static ScriptException throwURIError(Realm realm, Messages.Key key, String... args) {
        String message = format(realm, key, args);
        Scriptable nativeError = realm.getNativeError(NativeError.ErrorType.URIError);
        Object error = ((Constructor) nativeError).construct(message);
        return _throw(error);
    }
}
