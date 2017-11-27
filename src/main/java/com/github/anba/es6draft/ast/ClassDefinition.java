/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

import java.util.List;

import com.github.anba.es6draft.ast.scope.BlockScope;

/**
 * <h1>14 ECMAScript Language: Functions and Classes</h1>
 * <ul>
 * <li>14.5 Class Definitions
 * </ul>
 */
public interface ClassDefinition extends ScopedNode {
    /**
     * Returns the scope object for this node. May return {@code null} if this class definition does not create an
     * implicit scope.
     * 
     * @return the scope object or {@code null}
     */
    @Override
    BlockScope getScope();

    /**
     * Returns the scope object for this node. May return {@code null} if this class definition does not create an
     * implicit scope for its class body.
     * 
     * @return the scope object or {@code null}
     */
    BlockScope getBodyScope();

    /**
     * Returns the list of class decorators.
     * 
     * @return the list of decorators
     */
    List<Expression> getDecorators();

    /**
     * Returns the class name
     * 
     * @return the class name
     */
    String getClassName();

    /**
     * Returns the class name or {@code null} if not present.
     * 
     * @return the class name or {@code null}
     */
    BindingIdentifier getIdentifier();

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
     * Returns the constructor method.
     * 
     * @return the constructor method
     */
    MethodDefinition getConstructor();

    /**
     * Returns the call constructor method.
     * 
     * @return the call constructor method
     */
    MethodDefinition getCallConstructor();

    /**
     * Returns the list of class properties.
     * 
     * @return the list of class properties
     */
    List<PropertyDefinition> getProperties();

    /**
     * Sets the list of class properties.
     * 
     * @param properties
     *            the new list of class properties
     */
    void setProperties(List<PropertyDefinition> properties);

    /**
     * Returns the source string for this class.
     * 
     * @return the source string
     */
    String getSource();
}
