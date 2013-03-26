/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import com.github.anba.es6draft.runtime.Realm;

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
    ScriptObject getPrototype(Realm realm);

    /** [[SetPrototype]] (V) */
    boolean setPrototype(Realm realm, ScriptObject prototype);

    /** [[HasIntegrity]] (Level) */
    boolean hasIntegrity(Realm realm, IntegrityLevel level);

    /** [[SetIntegrity]] (Level) */
    boolean setIntegrity(Realm realm, IntegrityLevel level);

    /** [[HasOwnProperty]] (P) */
    boolean hasOwnProperty(Realm realm, String propertyKey);

    /** [[HasOwnProperty]] (P) */
    boolean hasOwnProperty(Realm realm, Symbol propertyKey);

    /** [[GetOwnProperty]] (P) */
    Property getOwnProperty(Realm realm, String propertyKey);

    /** [[GetOwnProperty]] (P) */
    Property getOwnProperty(Realm realm, Symbol propertyKey);

    /** [[HasProperty]](P) */
    boolean hasProperty(Realm realm, String propertyKey);

    /** [[HasProperty]](P) */
    boolean hasProperty(Realm realm, Symbol propertyKey);

    // FIXME: spec bug ([[Get]] missing in 8.1.6.2)

    /** [[Get]] (P, Receiver) */
    Object get(Realm realm, String propertyKey, Object receiver);

    /** [[Get]] (P, Receiver) */
    Object get(Realm realm, Symbol propertyKey, Object receiver);

    /** [[Set] (P, V, Receiver) */
    boolean set(Realm realm, String propertyKey, Object value, Object receiver);

    /** [[Set] (P, V, Receiver) */
    boolean set(Realm realm, Symbol propertyKey, Object value, Object receiver);

    /** [[Delete]] (P) */
    boolean delete(Realm realm, String propertyKey);

    /** [[Delete]] (P) */
    boolean delete(Realm realm, Symbol propertyKey);

    /** [[DefineOwnProperty]] (P, Desc) */
    boolean defineOwnProperty(Realm realm, String propertyKey, PropertyDescriptor desc);

    /** [[DefineOwnProperty]] (P, Desc) */
    boolean defineOwnProperty(Realm realm, Symbol propertyKey, PropertyDescriptor desc);

    /** [[Enumerate]] () */
    ScriptObject enumerate(Realm realm);

    /** [[OwnPropertyKeys]] ( ) */
    ScriptObject ownPropertyKeys(Realm realm);

}
