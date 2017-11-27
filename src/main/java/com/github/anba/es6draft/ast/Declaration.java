/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.2 Block
 * </ul>
 */
public abstract class Declaration extends StatementListItem {
    protected Declaration(long beginPosition, long endPosition) {
        super(beginPosition, endPosition);
    }

    /**
     * Returns {@code true} if this declaration creates an immutable binding.
     * 
     * @return {@code true} if a <code>const</code> declaration
     */
    public abstract boolean isConstDeclaration();
}
