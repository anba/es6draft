/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import java.util.Objects;

/**
 * <h1>6 ECMAScript Data Types and Values</h1><br>
 * <h2>6.1 ECMAScript Language Types</h2>
 * <ul>
 * <li>6.1.5 The Symbol Type
 * </ul>
 */
public final class Symbol {
    /** [[Description]] */
    private final String description;

    /**
     * Constructs a new symbol instance.
     * 
     * @param description
     *            the symbol description or {@code null}
     */
    public Symbol(String description) {
        this.description = description;
    }

    /**
     * [[Description]]
     *
     * @return the symbol description or {@code null}
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Symbol(" + Objects.toString(getDescription(), "") + ")";
    }
}
