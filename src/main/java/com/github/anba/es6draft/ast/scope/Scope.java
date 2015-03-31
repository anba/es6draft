/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.scope;

import com.github.anba.es6draft.ast.ScopedNode;

/**
 * Base interface for scope information.
 */
public interface Scope extends Iterable<Scope> {
    /**
     * Returns the parent scope.
     * 
     * @return the parent scope
     */
    Scope getParent();

    /**
     * Returns the enclosing top-level scope.
     * 
     * @return the top-level scope
     */
    TopLevelScope getTop();

    /**
     * Returns the {@link ScopedNode} for this scope object.
     * 
     * @return the node
     */
    ScopedNode getNode();

    /**
     * Returns <code>true</code> if {@code name} is statically declared in this scope.
     * 
     * @param name
     *            the variable name
     * @return <code>true</code> if {@code name} is declared
     */
    boolean isDeclared(Name name);

    /**
     * Returns the resolved name for {@code name}. {@code name} must be statically declared in this
     * scope.
     * 
     * @param name
     *            the variable name
     * @param lookupByName
     *            named lookup
     * @return the resolved name
     */
    Name resolveName(Name name, boolean lookupByName);

    /**
     * Returns <code>true</code> if the scope is emitted at runtime.
     * 
     * @return <code>true</code> if the scope is emitted
     */
    boolean isPresent();

    /**
     * Returns <code>true</code> for dynamically scoped objects, <code>false</code> otherwise.
     * 
     * @return <code>true</code> if a dynamic scope
     */
    boolean isDynamic();
}
