/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.7 Built-in Function Objects
 * </ul>
 */
public abstract class BuiltinFunction extends OrdinaryObject implements ScriptObject, Callable {
    public BuiltinFunction(Realm realm) {
        super(realm);
    }

    @Override
    public String toSource() {
        Property desc = ordinaryGetOwnProperty("name");
        assert desc != null && desc.isDataDescriptor() : "built-in functions have an own 'name' data property";
        Object name = desc.getValue();
        assert Type.isString(name) : "'name' is a string valued data property";
        return String.format("function %s() { /* native code */ }", Type.stringValue(name));
    }
}
