/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1><br>
 * <h2>13.3 Declarations and the Variable Statement</h2>
 * <ul>
 * <li>13.3.3 Destructuring Binding Patterns
 * </ul>
 */
public abstract class BindingPattern extends Binding {
    protected BindingPattern(long beginPosition, long endPosition) {
        super(beginPosition, endPosition);
    }
}
