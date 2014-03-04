/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.Set;

/**
 * Scope class for {@link FunctionNode} objects
 */
public interface FunctionScope extends TopLevelScope {
    @Override
    FunctionNode getNode();

    /**
     * Returns the set of parameter names.
     */
    Set<String> parameterNames();

    /**
     * Returns <code>true</code> for dynamically scoped objects, <code>false</code> otherwise.
     * <p>
     * A scope is considered dynamic if it can change during runtime, in other words that means it
     * contains a non-strict, direct-eval call.
     */
    boolean isDynamic();
}
