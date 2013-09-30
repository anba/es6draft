/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.builtins.ExoticSymbol;

/**
 * <h1>6 ECMAScript Data Types and Values</h1><br>
 * <h2>6.1 ECMAScript Language Types</h2><br>
 * <h3>6.1.6 The Object Type</h3>
 * <ul>
 * <li>6.1.6.2 Object Internal Methods and Internal Data Properties
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
    Property getOwnProperty(ExecutionContext cx, ExoticSymbol propertyKey);

    /** [[HasProperty]](P) */
    boolean hasProperty(ExecutionContext cx, String propertyKey);

    /** [[HasProperty]](P) */
    boolean hasProperty(ExecutionContext cx, ExoticSymbol propertyKey);

    /** [[Get]] (P, Receiver) */
    Object get(ExecutionContext cx, String propertyKey, Object receiver);

    /** [[Get]] (P, Receiver) */
    Object get(ExecutionContext cx, ExoticSymbol propertyKey, Object receiver);

    /** [[Set] (P, V, Receiver) */
    boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver);

    /** [[Set] (P, V, Receiver) */
    boolean set(ExecutionContext cx, ExoticSymbol propertyKey, Object value, Object receiver);

    /** [[Invoke]] (P, ArgumentsList, Receiver) */
    Object invoke(ExecutionContext cx, String propertyKey, Object[] arguments, Object receiver);

    /** [[Invoke]] (P, ArgumentsList, Receiver) */
    Object invoke(ExecutionContext cx, ExoticSymbol propertyKey, Object[] arguments, Object receiver);

    /** [[Delete]] (P) */
    boolean delete(ExecutionContext cx, String propertyKey);

    /** [[Delete]] (P) */
    boolean delete(ExecutionContext cx, ExoticSymbol propertyKey);

    /** [[DefineOwnProperty]] (P, Desc) */
    boolean defineOwnProperty(ExecutionContext cx, String propertyKey, PropertyDescriptor desc);

    /** [[DefineOwnProperty]] (P, Desc) */
    boolean defineOwnProperty(ExecutionContext cx, ExoticSymbol propertyKey, PropertyDescriptor desc);

    /** [[Enumerate]] () */
    ScriptObject enumerate(ExecutionContext cx);

    /** [[OwnPropertyKeys]] ( ) */
    ScriptObject ownPropertyKeys(ExecutionContext cx);
}
