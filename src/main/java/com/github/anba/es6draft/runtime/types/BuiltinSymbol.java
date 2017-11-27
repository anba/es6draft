/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import java.util.EnumMap;

/**
 * <h1>6 ECMAScript Data Types and Values</h1><br>
 * <h2>6.1.5 The Symbol Type</h2>
 * <ul>
 * <li>6.1.5.1 Well-Known Symbols
 * </ul>
 */
public enum BuiltinSymbol {
    NONE,

    /**
     * &#64;&#64;hasInstance
     */
    hasInstance,

    /**
     * &#64;&#64;isConcatSpreadable
     */
    isConcatSpreadable,

    /**
     * &#64;&#64;iterator
     */
    iterator,

    /**
     * &#64;&#64;match
     */
    match,

    /**
     * &#64;&#64;replace
     */
    replace,

    /**
     * &#64;&#64;search
     */
    search,

    /**
     * &#64;&#64;species
     */
    species,

    /**
     * &#64;&#64;split
     */
    split,

    /**
     * &#64;&#64;toPrimitive
     */
    toPrimitive,

    /**
     * &#64;&#64;toStringTag
     */
    toStringTag,

    /**
     * &#64;&#64;unscopables
     */
    unscopables,

    /**
     * &#64;&#64;observable
     */
    observable,

    /**
     * &#64;&#64;asyncIterator
     */
    asyncIterator,

    /**
     * &#64;&#64;matchAll
     */
    matchAll,

    ;

    /**
     * Returns the {@link Symbol} object for this {@link BuiltinSymbol}.
     * 
     * @return the built-in symbol
     */
    public final Symbol get() {
        assert this != NONE;
        return symbols.get(this);
    }

    @Override
    public String toString() {
        return "Symbol." + name();
    }

    private static final EnumMap<BuiltinSymbol, Symbol> symbols;

    static {
        EnumMap<BuiltinSymbol, Symbol> map = new EnumMap<>(BuiltinSymbol.class);
        for (BuiltinSymbol builtin : values()) {
            if (builtin != NONE) {
                String name = "Symbol." + builtin.name();
                map.put(builtin, new Symbol(name));
            }
        }
        symbols = map;
    }
}
