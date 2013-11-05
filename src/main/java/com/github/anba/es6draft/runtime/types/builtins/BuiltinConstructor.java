/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Constructor;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.3 Built-in Function Objects
 * </ul>
 */
public abstract class BuiltinConstructor extends BuiltinFunction implements Constructor {
    public BuiltinConstructor(Realm realm, String name) {
        super(realm, name);
    }

    @Override
    public final boolean isConstructor() {
        // built-in constructors are born with [[Construct]] already attached
        return true;
    }
}
