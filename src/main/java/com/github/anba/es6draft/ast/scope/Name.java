/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.scope;

/**
 * 
 */
public final class Name {
    private final String identifier;
    private Scope scope;

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

    public void resolve(Scope scope) {
        assert this.scope == null;
        this.scope = scope;
    }

    public boolean isResolved() {
        return scope != null;
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
}
