/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>15 ECMAScript Language: Modules and Scripts</h1>
 * <ul>
 * <li>15.1 Modules
 * </ul>
 */
public abstract class ModuleItem extends AstNode {
    protected ModuleItem(long beginPosition, long endPosition) {
        super(beginPosition, endPosition);
    }
}
