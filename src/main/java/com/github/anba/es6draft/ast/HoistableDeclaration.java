/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import com.github.anba.es6draft.ast.scope.Name;

/**
 * <h1>13 ECMAScript Language: Statements and Declarations</h1>
 * <ul>
 * <li>13.0 HoistableDeclaration
 * </ul>
 */
public abstract class HoistableDeclaration extends Declaration implements FunctionNode {
    protected HoistableDeclaration(long beginPosition, long endPosition) {
        super(beginPosition, endPosition);
    }

    /**
     * Returns the binding identifier of this hoistable declaration or {@code null} for anonymous
     * default export declarations.
     * 
     * @return the binding identifier or {@code null}
     */
    public abstract BindingIdentifier getIdentifier();

    /**
     * Returns the bound name of this hoistable declaration.
     * 
     * @return the bound name
     */
    public abstract Name getName();
}
