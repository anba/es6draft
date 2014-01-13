/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * <h1>6 ECMAScript Data Types and Values</h1><br>
 * <h2>6.1 ECMAScript Language Types</h2><br>
 * <h3>6.1.7 The Object Type</h3>
 * <ul>
 * <li>6.1.7.2 Object Internal Methods and Internal Slots
 * </ul>
 */
public interface ScriptObject {
    /** [[GetPrototypeOf]] ( ) */
    ScriptObject getPrototypeOf(ExecutionContext cx);

    /** [[SetPrototypeOf]] (V) */
    boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype);

    /** [[IsExtensible]] () */
    boolean isExtensible(ExecutionContext cx);

    /** [[PreventExtensions]] () */
    boolean preventExtensions(ExecutionContext cx);

    /** [[GetOwnProperty]] (P) */
    Property getOwnProperty(ExecutionContext cx, String propertyKey);

    /** [[GetOwnProperty]] (P) */
    Property getOwnProperty(ExecutionContext cx, Symbol propertyKey);

    /** [[HasProperty]](P) */
    boolean hasProperty(ExecutionContext cx, String propertyKey);

    /** [[HasProperty]](P) */
    boolean hasProperty(ExecutionContext cx, Symbol propertyKey);

    /** [[Get]] (P, Receiver) */
    Object get(ExecutionContext cx, String propertyKey, Object receiver);

    /** [[Get]] (P, Receiver) */
    Object get(ExecutionContext cx, Symbol propertyKey, Object receiver);

    /** [[Set] (P, V, Receiver) */
    boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver);

    /** [[Set] (P, V, Receiver) */
    boolean set(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver);

    /** [[Delete]] (P) */
    boolean delete(ExecutionContext cx, String propertyKey);

    /** [[Delete]] (P) */
    boolean delete(ExecutionContext cx, Symbol propertyKey);

    /** [[DefineOwnProperty]] (P, Desc) */
    boolean defineOwnProperty(ExecutionContext cx, String propertyKey, PropertyDescriptor desc);

    /** [[DefineOwnProperty]] (P, Desc) */
    boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey, PropertyDescriptor desc);

    /** [[Enumerate]] () */
    ScriptObject enumerate(ExecutionContext cx);

    /** [[OwnPropertyKeys]] ( ) */
    ScriptObject ownPropertyKeys(ExecutionContext cx);
}
