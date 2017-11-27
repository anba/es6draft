/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.scope.BlockScope;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.15 The try Statement
 * </ul>
 */
public interface CatchClause extends ScopedNode {
    @Override
    BlockScope getScope();

    /**
     * Returns the optional catch parameter binding node.
     * 
     * @return the catch parameter or {@code null}
     */
    Binding getCatchParameter();

    /**
     * Returns the catch block statement.
     * 
     * @return the catch block
     */
    BlockStatement getCatchBlock();
}
