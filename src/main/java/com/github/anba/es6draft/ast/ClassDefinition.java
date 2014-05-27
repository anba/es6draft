/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.5 Class Definitions
 * </ul>
 */
public interface ClassDefinition extends ScopedNode {
    /**
     * Returns the class name or {@code null} if not present.
     * 
     * @return the class name or {@code null}
     */
    BindingIdentifier getName();

    /**
     * Returns the class heritage expression or {@code null} if not present.
     * 
     * @return the class heritage expression or {@code null}
     */
    Expression getHeritage();

    /**
     * Returns the list of class methods in source order.
     * 
     * @return the list of class methods
     */
    List<MethodDefinition> getMethods();

    /**
     * Returns the list of static class methods.
     * 
     * @return the list of static class methods
     */
    List<MethodDefinition> getStaticMethods();

    /**
     * Returns the list of prototype class methods.
     * 
     * @return the list of prototype class methods
     */
    List<MethodDefinition> getPrototypeMethods();
}
