/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import java.util.EnumMap;

import com.github.anba.es6draft.runtime.types.builtins.ExoticSymbolObject;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.1 ECMAScript Language Types</h2><br>
 * <h3>8.1.6 The Object Type</h3>
 * <ul>
 * <li>8.1.6.3 Well-Known Symbols and Intrinsics
 * </ul>
 */
public enum BuiltinSymbol {
    NONE, //

    /**
     * @@create
     */
    create,

    /**
     * @@hasInstance
     */
    hasInstance,

    /**
     * @@iterator
     */
    iterator,

    /**
     * @@ToPrimitive
     */
    ToPrimitive,

    /**
     * @@toStringTag
     */
    toStringTag,

    /**
     * @@elementGet
     */
    elementGet,

    /**
     * @@elementSet
     */
    elementSet,

    ;

    public final Symbol get() {
        assert this != NONE;
        return symbols.get(this);
    }

    private static final EnumMap<BuiltinSymbol, Symbol> symbols;
    static {
        EnumMap<BuiltinSymbol, Symbol> map = new EnumMap<>(BuiltinSymbol.class);
        for (BuiltinSymbol builtin : values()) {
            if (builtin != NONE) {
                map.put(builtin, new ExoticSymbolObject(new Object()));
            }
        }
        symbols = map;
    }
}
