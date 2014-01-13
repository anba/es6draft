/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

/**
 * <h1>6 ECMAScript Data Types and Values</h1><br>
 * <h2>6.1 ECMAScript Language Types</h2>
 * <ul>
 * <li>6.1.1 The Undefined Type
 * </ul>
 */
public final class Undefined {
    public static final Undefined UNDEFINED = new Undefined();

    private Undefined() {
    }

    @Override
    public String toString() {
        return "undefined";
    }
}
