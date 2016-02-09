/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1>
 * <ul>
 * <li>15.2 Modules
 * </ul>
 */
public abstract class ModuleItem extends AstNode {
    private boolean completionValue = true;

    protected ModuleItem(long beginPosition, long endPosition) {
        super(beginPosition, endPosition);
    }

    /**
     * Returns {@code true} if this module item has a completion value.
     * 
     * @return {@code true} if this module item has a completion value
     */
    public boolean hasCompletionValue() {
        return completionValue;
    }

    /**
     * Sets the completion value marker.
     * 
     * @param completionValue
     *            the new value
     */
    public void setCompletionValue(boolean completionValue) {
        this.completionValue = completionValue;
    }
}
