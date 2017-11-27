/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.scope;

/**
 * 
 */
public final class Name implements Cloneable {
    public static final String DEFAULT_EXPORT = "*default*";
    private final String identifier;

    /**
     * Constructs a new Name object.
     * 
     * @param identifier
     *            the identifier string
     */
    public Name(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the identifier string.
     * 
     * @return the identifier string
     */
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != Name.class) {
            return false;
        }
        return identifier.equals(((Name) obj).identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return identifier;
    }

    @Override
    public Name clone() {
        return new Name(identifier);
    }
}
