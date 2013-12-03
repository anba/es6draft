/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * Super-interface for {@link Script} and {@link FunctionNode}
 */
public interface TopLevelNode extends ScopedNode {
    List<? extends ModuleItem> getStatements();
}
