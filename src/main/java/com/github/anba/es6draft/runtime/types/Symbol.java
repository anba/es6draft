/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.1 ECMAScript Language Types</h2>
 * <ul>
 * <li>8.1.6 The Symbol Type
 * </ul>
 */
public final class Symbol {
    private final String name;
    private final boolean _private;

    public Symbol(String name, boolean _private) {
        this.name = name;
        this._private = _private;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * [[Private]]
     */
    public boolean isPrivate() {
        return _private;
    }
}
