/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
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
public class ErrorObject extends OrdinaryObject implements ScriptObject {
    private boolean initialised = false;

    public ErrorObject(Realm realm) {
        super(realm);
    }

    public boolean isInitialised() {
        return initialised;
    }

    public void initialise() {
        assert !this.initialised : "ErrorObject already initialised";
        this.initialised = true;
    }

    @Override
    public String toString() {
        try {
            Object toString = Get(realm(), this, "toString");
            if (toString instanceof Callable) {
                return ToFlatString(realm(), ((Callable) toString).call(this));
            }
        } catch (RuntimeException e) {
        }
        return "???";
    }
}
