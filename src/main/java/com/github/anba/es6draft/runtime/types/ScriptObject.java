/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.1 ECMAScript Language Types</h2><br>
 * <h3>8.1.6 The Object Type</h3>
 * <ul>
 * <li>8.1.6.2 Object Internal Methods and Internal Data Properties
 * </ul>
 */
public interface ScriptObject {
    /** [[GetPrototype]] ( ) */
    ScriptObject getPrototype(ExecutionContext cx);

    /** [[SetPrototype]] (V) */
    boolean setPrototype(ExecutionContext cx, ScriptObject prototype);

    /** [[HasIntegrity]] (Level) */
    boolean hasIntegrity(ExecutionContext cx, IntegrityLevel level);

    /** [[SetIntegrity]] (Level) */
    boolean setIntegrity(ExecutionContext cx, IntegrityLevel level);

    /** [[HasOwnProperty]] (P) */
    boolean hasOwnProperty(ExecutionContext cx, String propertyKey);

    /** [[HasOwnProperty]] (P) */
    boolean hasOwnProperty(ExecutionContext cx, Symbol propertyKey);

    /** [[GetOwnProperty]] (P) */
    Property getOwnProperty(ExecutionContext cx, String propertyKey);

    /** [[GetOwnProperty]] (P) */
    Property getOwnProperty(ExecutionContext cx, Symbol propertyKey);

    /** [[HasProperty]](P) */
    boolean hasProperty(ExecutionContext cx, String propertyKey);

    /** [[HasProperty]](P) */
    boolean hasProperty(ExecutionContext cx, Symbol propertyKey);

    // FIXME: spec bug ([[Get]] missing in 8.1.6.2)

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
