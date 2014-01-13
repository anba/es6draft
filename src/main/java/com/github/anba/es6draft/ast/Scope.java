/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Base interface for scope information
 */
public interface Scope {
    /**
     * Returns the parent scope.
     */
    Scope getParent();

    /**
     * Returns the {@link ScopedNode} for this scope object.
     */
    ScopedNode getNode();

    /**
     * Returns <code>true</code> for dynamically scoped objects, <code>false</code> otherwise.
     * <p>
     * A scope is considered dynamic if it can change over the course of runtime, this applies to,
     * for example, a {@link FunctionScope} with non-strict, direct-eval calls or the
     * {@link BlockScope} of a {@link WithStatement}.
     */
    boolean isDynamic();
}
