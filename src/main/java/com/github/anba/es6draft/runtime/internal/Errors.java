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
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 *
 */
public final class Errors {
    private Errors() {
    }

    private static Object newError(Realm realm, Intrinsics constructor, Messages.Key key) {
        String message = realm.message(key);
        ScriptObject nativeError = realm.getIntrinsic(constructor);
        return ((Constructor) nativeError).construct(message);
    }

    private static Object newError(Realm realm, Intrinsics constructor, Messages.Key key,
            String... args) {
        MessageFormat format = new MessageFormat(realm.message(key), realm.getLocale());
        String message = format.format(args);
        ScriptObject nativeError = realm.getIntrinsic(constructor);
        return ((Constructor) nativeError).construct(message);
    }

    public static ScriptException throwInternalError(Realm realm, Messages.Key key) {
        return _throw(newError(realm, Intrinsics.InternalError, key));
    }

    public static ScriptException throwInternalError(Realm realm, Messages.Key key, String... args) {
        return _throw(newError(realm, Intrinsics.InternalError, key, args));
    }

    public static ScriptException throwTypeError(Realm realm, Messages.Key key) {
        return _throw(newError(realm, Intrinsics.TypeError, key));
    }

    public static ScriptException throwTypeError(Realm realm, Messages.Key key, String... args) {
        return _throw(newError(realm, Intrinsics.TypeError, key, args));
    }

    public static ScriptException throwReferenceError(Realm realm, Messages.Key key) {
        return _throw(newError(realm, Intrinsics.ReferenceError, key));
    }

    public static ScriptException throwReferenceError(Realm realm, Messages.Key key, String... args) {
        return _throw(newError(realm, Intrinsics.ReferenceError, key, args));
    }

    public static ScriptException throwSyntaxError(Realm realm, Messages.Key key) {
        return _throw(newError(realm, Intrinsics.SyntaxError, key));
    }

    public static ScriptException throwSyntaxError(Realm realm, Messages.Key key, String... args) {
        return _throw(newError(realm, Intrinsics.SyntaxError, key, args));
    }

    public static ScriptException throwRangeError(Realm realm, Messages.Key key) {
        return _throw(newError(realm, Intrinsics.RangeError, key));
    }

    public static ScriptException throwRangeError(Realm realm, Messages.Key key, String... args) {
        return _throw(newError(realm, Intrinsics.RangeError, key, args));
    }

    public static ScriptException throwURIError(Realm realm, Messages.Key key) {
        return _throw(newError(realm, Intrinsics.URIError, key));
    }

    public static ScriptException throwURIError(Realm realm, Messages.Key key, String... args) {
        return _throw(newError(realm, Intrinsics.URIError, key, args));
    }
}
