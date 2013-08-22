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
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.7 Built-in Function Objects
 * </ul>
 */
public abstract class BuiltinConstructor extends BuiltinFunction implements Constructor {
    public BuiltinConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public final boolean isConstructor() {
        // built-in constructors are born with [[Construct]] already attached
        return true;
    }
}
