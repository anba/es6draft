/**
 * Copyright (c) 2012-2015 André Bargull
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
public abstract class Statement extends StatementListItem {
    protected Statement(long beginPosition, long endPosition) {
        super(beginPosition, endPosition);
    }
}
