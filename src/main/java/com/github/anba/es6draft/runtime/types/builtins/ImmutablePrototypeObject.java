/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 *
 */
public abstract class ImmutablePrototypeObject extends OrdinaryObject {
    /**
     * Constructs a new Immutable Prototype Object instance.
     * 
     * @param realm
     *            the realm object
     * @param prototype
     *            the initial prototype
     */
    public ImmutablePrototypeObject(Realm realm, ScriptObject prototype) {
        super(realm);
        setPrototype(prototype);
    }

    @Override
    public boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype) {
        /* step 1 */
        ScriptObject current = getPrototype();
        /* step 2 */
        if (prototype == current) { // SameValue(prototype, current)
            return true;
        }
        /* step 3 */
        return false;
    }
}
