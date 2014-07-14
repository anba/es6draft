/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast.scope;

import com.github.anba.es6draft.ast.Script;

/**
 * Scope class for {@link Script} objects
 */
public interface ScriptScope extends TopLevelScope {
    @Override
    Script getNode();
}
