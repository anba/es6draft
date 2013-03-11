/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.1 ECMAScript Language Types</h2><br>
 * <h3>8.1.6 The Object Type</h3>
 * <ul>
 * <li>8.1.6.2 Object Internal Methods and Internal Data Properties
 * </ul>
 */
public interface Scriptable {
    /**
     * {@link OrdinaryObject#ObjectCreate(Realm, Scriptable, Intrinsics)}
     */
    Scriptable newInstance(Realm realm);

    /**
     * [[BuiltinBrand]]
     */
    BuiltinBrand getBuiltinBrand();

    /** [[GetPrototype]] ( ) */
    Scriptable getPrototype();

    /** [[SetPrototype]] (V) */
    boolean setPrototype(Scriptable prototype);

    /** [[HasIntegrity]] (Level) */
    boolean hasIntegrity(IntegrityLevel level);

    /** [[SetIntegrity]] (Level) */
    boolean setIntegrity(IntegrityLevel level);

    /** [[HasOwnProperty]] (P) */
    boolean hasOwnProperty(String propertyKey);

    /** [[HasOwnProperty]] (P) */
    boolean hasOwnProperty(Symbol propertyKey);

    /** [[GetOwnProperty]] (P) */
    Property getOwnProperty(String propertyKey);

    /** [[GetOwnProperty]] (P) */
    Property getOwnProperty(Symbol propertyKey);

    /** [[HasProperty]](P) */
    boolean hasProperty(String propertyKey);

    /** [[HasProperty]](P) */
    boolean hasProperty(Symbol propertyKey);

    // FIXME: spec bug ([[Get]] missing in 8.1.6.2)

    /** [[Get]] (P, Receiver) */
    Object get(String propertyKey, Object receiver);

    /** [[Get]] (P, Receiver) */
    Object get(Symbol propertyKey, Object receiver);

    /** [[Set] (P, V, Receiver) */
    boolean set(String propertyKey, Object value, Object receiver);

    /** [[Set] (P, V, Receiver) */
    boolean set(Symbol propertyKey, Object value, Object receiver);

    /** [[Delete]] (P) */
    boolean delete(String propertyKey);

    /** [[Delete]] (P) */
    boolean delete(Symbol propertyKey);

    /** [[DefineOwnProperty]] (P, Desc) */
    boolean defineOwnProperty(String propertyKey, PropertyDescriptor desc);

    /** [[DefineOwnProperty]] (P, Desc) */
    boolean defineOwnProperty(Symbol propertyKey, PropertyDescriptor desc);

    /** [[Enumerate]] () */
    Scriptable enumerate();

    /** [[OwnPropertyKeys]] ( ) */
    Scriptable ownPropertyKeys();

}
