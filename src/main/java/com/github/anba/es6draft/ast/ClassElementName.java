/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.ast;

/**
 * Extension: Class Fields
 */
public interface ClassElementName extends Node {
    /**
     * Returns the property name representation of this class element name.
     * 
     * @return the property name representation
     */
    PropertyName toPropertyName();
}
