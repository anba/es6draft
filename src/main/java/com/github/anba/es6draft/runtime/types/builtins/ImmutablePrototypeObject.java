/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Slots</h2>
 * <ul>
 * <li>9.4.7 Immutable Prototype Exotic Objects
 * </ul>
 */
public class ImmutablePrototypeObject extends OrdinaryObject {
    /**
     * Constructs a new Immutable Prototype Object instance.
     * 
     * @param realm
     *            the realm object
     * @param prototype
     *            the initial prototype
     */
    public ImmutablePrototypeObject(Realm realm, ScriptObject prototype) {
        super(realm, prototype);
    }

    /**
     * 9.4.7.1 [[SetPrototypeOf]] (V)
     */
    @Override
    public final boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype) {
        /* step 1 */
        return SetImmutablePrototype(this, prototype);
    }

    /**
     * 9.4.7.2 SetImmutablePrototype ( O, V )
     * 
     * @param obj
     *            the script object
     * @param prototype
     *            the new prototype object
     * @return {@code true} if the prototype was successfully updated
     */
    public static final boolean SetImmutablePrototype(OrdinaryObject obj, ScriptObject prototype) {
        /* step 1 (not applicable) */
        /* step 2 */
        ScriptObject current = obj.getPrototype();
        /* step 3 */
        if (prototype == current) { // SameValue(prototype, current)
            return true;
        }
        /* step 4 */
        return false;
    }
}
