/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

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
        // [[Construct]] always present for built-in constructors
        return true;
    }

    @Override
    public final ScriptObject tailConstruct(ExecutionContext callerContext, Object... args) {
        return construct(callerContext, args);
    }
}
