/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import java.util.HashMap;

import com.github.anba.es6draft.runtime.types.Symbol;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.4 Symbol Objects</h2>
 * <ul>
 * <li>GlobalSymbolRegistry Record
 * </ul>
 */
public final class GlobalSymbolRegistry {
    private final HashMap<String, Symbol> elements = new HashMap<>();

    @Override
    public String toString() {
        return String.format("GlobalSymbolRegistry=%s", elements);
    }

    /**
     * Returns the symbol which is mapped to {@code key}, or creates a new mapping.
     * 
     * @param key
     *            the symbol string key
     * @return the mapped symbol
     */
    public Symbol getSymbol(String key) {
        assert key != null : "key must not be null";
        Symbol symbol = elements.get(key);
        if (symbol != null) {
            return symbol;
        }
        Symbol newSymbol = new Symbol(key);
        elements.put(key, newSymbol);
        return newSymbol;
    }

    /**
     * Returns the key which is mapped to {@code symbol}, or returns <code>null</code> if there is
     * no mapping.
     * 
     * @param symbol
     *            the global symbol
     * @return the symbol string key or {@code null} if the symbol is not in the registry
     */
    public String getKey(Symbol symbol) {
        assert symbol != null : "symbol must not be null";
        String key = symbol.getDescription();
        Symbol existingSymbol = elements.get(key);
        if (existingSymbol == symbol) {
            return key;
        }
        return null;
    }
}
