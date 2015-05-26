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
public final class Name implements Cloneable {
    public static final String DEFAULT_EXPORT = "*default*";
    private final String identifier;
    private Scope scope;
    private boolean globalName;
    private boolean lookupByName;

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

    /**
     * Returns the declaring scope or {@code null}.
     * 
     * @return the declaring scope or {@code null}
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Returns {@code true} if this name requires named lookup.
     * 
     * @return {@code true} if named lookup required
     */
    public boolean isLookupByName() {
        return lookupByName;
    }

    /**
     * Returns {@code true} if this name references a global identifier.
     * 
     * @return {@code true} if global name
     */
    public boolean isGlobal() {
        return globalName;
    }

    /**
     * Returns {@code true} if this name references a local identifier.
     * 
     * @return {@code true} if local name
     */
    public boolean isLocal() {
        return scope != null && !globalName && !lookupByName;
    }

    /**
     * Returns {@code true} if this name is resolved.
     * 
     * @return {@code true} if resolved
     */
    public boolean isResolved() {
        return scope != null || globalName;
    }

    /**
     * Resolves this name.
     * 
     * @param scope
     *            the declaring scope
     * @param lookupByName
     *            {@code true} if named lookup required
     * @return {@code true} on success
     */
    public boolean resolve(Scope scope, boolean lookupByName) {
        assert scope != null && !globalName;
        this.lookupByName |= lookupByName;
        if (this.scope == null) {
            this.scope = scope;
            return true;
        }
        assert this.scope == scope : String.format("%s != %s", this.scope, scope);
        return false;
    }

    /**
     * Marks this name as global.
     * 
     * @return this name
     */
    public Name asGlobalName() {
        assert this.scope == null;
        this.globalName = true;
        return this;
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
