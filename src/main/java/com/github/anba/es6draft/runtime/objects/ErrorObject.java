/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.11 Error Objects</h2>
 * <ul>
 * <li>15.11.5 Properties of Error Instances
 * <li>15.11.7 NativeError Object Structure
 * <ul>
 * <li>15.11.7.5 Properties of NativeError Instances
 * </ul>
 * </ul>
 */
public class ErrorObject extends OrdinaryObject {
    private final Realm realm;
    private boolean initialised = false;
    private ScriptException exception = null;

    public ErrorObject(Realm realm) {
        super(realm);
        this.realm = realm;
        this.exception = new ScriptException(this);
    }

    public boolean isInitialised() {
        return initialised;
    }

    public void initialise() {
        assert !this.initialised : "ErrorObject already initialised";
        this.initialised = true;
    }

    public ScriptException getException() {
        return exception;
    }

    @Override
    public String toString() {
        try {
            ExecutionContext cx = realm.defaultContext();
            Object toString = Get(cx, this, "toString");
            if (toString instanceof Callable) {
                return ToFlatString(cx, ((Callable) toString).call(cx, this));
            }
        } catch (RuntimeException e) {
        }
        return "???";
    }
}
