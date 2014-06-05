/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Base interface for scope information.
 */
public interface Scope {
    /**
     * Returns the parent scope.
     * 
     * @return the parent scope
     */
    Scope getParent();

    /**
     * Returns the {@link ScopedNode} for this scope object.
     * 
     * @return the node
     */
    ScopedNode getNode();

    /**
     * Returns <code>true</code> if {@code name} is declared in this scope.
     * 
     * @param name
     *            the variable name
     * @return <code>true</code> if {@code name} is declared
     */
    boolean isDeclared(String name);
}
