/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

/**
 * {@link ScriptObject} sub-interface for objects with a [[CreateAction]] internal slot.
 */
public interface Creatable<OBJECT extends ScriptObject> extends ScriptObject {
    /**
     * Returns the {@link CreateAction} object which specifies how to create new instances.
     * 
     * @return the create action operation or {@code null}
     */
    CreateAction<? extends OBJECT> createAction();
}
