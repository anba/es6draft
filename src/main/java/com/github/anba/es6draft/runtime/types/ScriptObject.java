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
    /**
     * [[GetPrototypeOf]] ( )
     * 
     * @param cx
     *            the execution context
     * @return the prototype object or {@code null}
     */
    ScriptObject getPrototypeOf(ExecutionContext cx);

    /**
     * [[SetPrototypeOf]] (V)
     * 
     * @param cx
     *            the execution context
     * @param prototype
     *            the new prototype object
     * @return {@code true} if the prototype was successfully updated
     */
    boolean setPrototypeOf(ExecutionContext cx, ScriptObject prototype);

    /**
     * [[IsExtensible]] ()
     * 
     * @param cx
     *            the execution context
     * @return {@code true} if the object is extensible
     */
    boolean isExtensible(ExecutionContext cx);

    /**
     * [[PreventExtensions]] ()
     *
     * @param cx
     *            the execution context
     * @return {@code true} on success
     */
    boolean preventExtensions(ExecutionContext cx);

    /**
     * [[GetOwnProperty]] (P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return the property or {@code null} if none found
     */
    Property getOwnProperty(ExecutionContext cx, String propertyKey);

    /**
     * [[GetOwnProperty]] (P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return the property or {@code null} if none found
     */
    Property getOwnProperty(ExecutionContext cx, Symbol propertyKey);

    /**
     * [[HasProperty]](P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was found
     */
    boolean hasProperty(ExecutionContext cx, String propertyKey);

    /**
     * [[HasProperty]](P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was found
     */
    boolean hasProperty(ExecutionContext cx, Symbol propertyKey);

    /**
     * [[Get]] (P, Receiver)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param receiver
     *            the receiver object
     * @return the property value
     */
    Object get(ExecutionContext cx, String propertyKey, Object receiver);

    /**
     * [[Get]] (P, Receiver)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param receiver
     *            the receiver object
     * @return the property value
     */
    Object get(ExecutionContext cx, Symbol propertyKey, Object receiver);

    /**
     * [[Set] (P, V, Receiver)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param value
     *            the new property value
     * @param receiver
     *            the receiver object
     * @return {@code true} on success
     */
    boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver);

    /**
     * [[Set] (P, V, Receiver)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param value
     *            the new property value
     * @param receiver
     *            the receiver object
     * @return {@code true} on success
     */
    boolean set(ExecutionContext cx, Symbol propertyKey, Object value, Object receiver);

    /**
     * [[Delete]] (P)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was successfully deleted
     */
    boolean delete(ExecutionContext cx, String propertyKey);

    /**
     * [[Delete]] (P)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was successfully deleted
     */
    boolean delete(ExecutionContext cx, Symbol propertyKey);

    /**
     * [[DefineOwnProperty]] (P, Desc)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param desc
     *            the property descriptor
     * @return {@code true} if the property was successfully defined
     */
    boolean defineOwnProperty(ExecutionContext cx, String propertyKey, PropertyDescriptor desc);

    /**
     * [[DefineOwnProperty]] (P, Desc)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @param desc
     *            the property descriptor
     * @return {@code true} if the property was successfully defined
     */
    boolean defineOwnProperty(ExecutionContext cx, Symbol propertyKey, PropertyDescriptor desc);

    /**
     * [[Enumerate]] ()
     *
     * @param cx
     *            the execution context
     * @return the enumeration iterator object
     */
    ScriptObject enumerate(ExecutionContext cx);

    /**
     * [[OwnPropertyKeys]] ( )
     *
     * @param cx
     *            the execution context
     * @return the properties iterator object
     */
    ScriptObject ownPropertyKeys(ExecutionContext cx);
}
