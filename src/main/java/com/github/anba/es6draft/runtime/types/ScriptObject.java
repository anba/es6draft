/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.IndexedMap;

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
    Property getOwnProperty(ExecutionContext cx, long propertyKey);

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
     * [[GetOwnProperty]] (P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return the property or {@code null} if none found
     */
    default Property getOwnProperty(ExecutionContext cx, Object propertyKey) {
        if (propertyKey instanceof String) {
            long index = IndexedMap.toIndex((String) propertyKey);
            if (IndexedMap.isIndex(index)) {
                return getOwnProperty(cx, index);
            }
            return getOwnProperty(cx, (String) propertyKey);
        }
        return getOwnProperty(cx, (Symbol) propertyKey);
    }

    /**
     * [[HasProperty]](P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was found
     */
    boolean hasProperty(ExecutionContext cx, long propertyKey);

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
     * [[HasProperty]](P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was found
     */
    default boolean hasProperty(ExecutionContext cx, Object propertyKey) {
        if (propertyKey instanceof String) {
            long index = IndexedMap.toIndex((String) propertyKey);
            if (IndexedMap.isIndex(index)) {
                return hasProperty(cx, index);
            }
            return hasProperty(cx, (String) propertyKey);
        }
        return hasProperty(cx, (Symbol) propertyKey);
    }

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
    Object get(ExecutionContext cx, long propertyKey, Object receiver);

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
    default Object get(ExecutionContext cx, Object propertyKey, Object receiver) {
        if (propertyKey instanceof String) {
            long index = IndexedMap.toIndex((String) propertyKey);
            if (IndexedMap.isIndex(index)) {
                return get(cx, index, receiver);
            }
            return get(cx, (String) propertyKey, receiver);
        }
        return get(cx, (Symbol) propertyKey, receiver);
    }

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
    boolean set(ExecutionContext cx, long propertyKey, Object value, Object receiver);

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
    default boolean set(ExecutionContext cx, Object propertyKey, Object value, Object receiver) {
        if (propertyKey instanceof String) {
            long index = IndexedMap.toIndex((String) propertyKey);
            if (IndexedMap.isIndex(index)) {
                return set(cx, index, value, receiver);
            }
            return set(cx, (String) propertyKey, value, receiver);
        }
        return set(cx, (Symbol) propertyKey, value, receiver);
    }

    /**
     * [[Delete]] (P)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was successfully deleted
     */
    boolean delete(ExecutionContext cx, long propertyKey);

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
     * [[Delete]] (P)
     *
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return {@code true} if the property was successfully deleted
     */
    default boolean delete(ExecutionContext cx, Object propertyKey) {
        if (propertyKey instanceof String) {
            long index = IndexedMap.toIndex((String) propertyKey);
            if (IndexedMap.isIndex(index)) {
                return delete(cx, index);
            }
            return delete(cx, (String) propertyKey);
        }
        return delete(cx, (Symbol) propertyKey);
    }

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
    boolean defineOwnProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc);

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
    default boolean defineOwnProperty(ExecutionContext cx, Object propertyKey, PropertyDescriptor desc) {
        if (propertyKey instanceof String) {
            long index = IndexedMap.toIndex((String) propertyKey);
            if (IndexedMap.isIndex(index)) {
                return defineOwnProperty(cx, index, desc);
            }
            return defineOwnProperty(cx, (String) propertyKey, desc);
        }
        return defineOwnProperty(cx, (Symbol) propertyKey, desc);
    }

    /**
     * [[OwnPropertyKeys]] ( )
     *
     * @param cx
     *            the execution context
     * @return the list of own property keys
     */
    List<?> ownPropertyKeys(ExecutionContext cx);

    /**
     * [[OwnPropertyKeys]] ( )
     *
     * @param cx
     *            the execution context
     * @return the list of own string-valued property keys
     */
    default List<String> ownPropertyNames(ExecutionContext cx) {
        List<?> ownKeys = ownPropertyKeys(cx);
        ArrayList<String> ownNames = new ArrayList<>();
        for (Object key : ownKeys) {
            if (key instanceof String) {
                ownNames.add((String) key);
            }
        }
        return ownNames;
    }

    /**
     * [[OwnPropertyKeys]] ( )
     *
     * @param cx
     *            the execution context
     * @return the list of own symbol-valued property keys
     */
    default List<Symbol> ownPropertySymbols(ExecutionContext cx) {
        List<?> ownKeys = ownPropertyKeys(cx);
        ArrayList<Symbol> ownSymbols = new ArrayList<>();
        for (Object key : ownKeys) {
            if (key instanceof Symbol) {
                ownSymbols.add((Symbol) key);
            }
        }
        return ownSymbols;
    }

    /**
     * [[OwnPropertyKeys]] ( )
     *
     * @param cx
     *            the execution context
     * @return the enumerable keys iterator
     * @see ScriptObject#isEnumerableOwnProperty(ExecutionContext, String)
     */
    default Iterator<String> ownEnumerablePropertyKeys(ExecutionContext cx) {
        List<?> ownKeys = ownPropertyKeys(cx);
        List<String> enumerableKeys = new ArrayList<>();
        for (Object key : ownKeys) {
            if (key instanceof String) {
                enumerableKeys.add((String) key);
            }
        }
        return enumerableKeys.iterator();
    }

    /**
     * Enumerability states.
     */
    enum Enumerability {
        Enumerable, NonEnumerable, Deleted;

        public static Enumerability isEnumerable(boolean enumerable) {
            return enumerable ? Enumerable : NonEnumerable;
        }
    }

    /**
     * [[GetOwnProperty]] (P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return the enumerability kind
     */
    default Enumerability isEnumerableOwnProperty(ExecutionContext cx, String propertyKey) {
        Property prop = getOwnProperty(cx, (Object) propertyKey);
        if (prop == null) {
            return Enumerability.Deleted;
        }
        return Enumerability.isEnumerable(prop.isEnumerable());
    }

    /**
     * 7.3.11 HasOwnProperty (O, P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return the enumerability kind
     */
    default boolean hasOwnProperty(ExecutionContext cx, Object propertyKey) {
        if (propertyKey instanceof String) {
            long index = IndexedMap.toIndex((String) propertyKey);
            if (IndexedMap.isIndex(index)) {
                return hasOwnProperty(cx, index);
            }
            return hasOwnProperty(cx, (String) propertyKey);
        }
        return hasOwnProperty(cx, (Symbol) propertyKey);
    }

    /**
     * 7.3.11 HasOwnProperty (O, P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return the enumerability kind
     */
    default boolean hasOwnProperty(ExecutionContext cx, long propertyKey) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        Property desc = getOwnProperty(cx, propertyKey);
        /* steps 4-5 */
        return desc != null;
    }

    /**
     * 7.3.11 HasOwnProperty (O, P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return the enumerability kind
     */
    default boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        Property desc = getOwnProperty(cx, propertyKey);
        /* steps 4-5 */
        return desc != null;
    }

    /**
     * 7.3.11 HasOwnProperty (O, P)
     * 
     * @param cx
     *            the execution context
     * @param propertyKey
     *            the property key
     * @return the enumerability kind
     */
    default boolean hasOwnProperty(ExecutionContext cx, Symbol propertyKey) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        Property desc = getOwnProperty(cx, propertyKey);
        /* steps 4-5 */
        return desc != null;
    }

    /**
     * Returns the class name of this script object. (Used in {@code Object.prototype.toString}.)
     * 
     * @return the class name
     */
    default String className() {
        return "Object";
    }

    /**
     * Returns a private name property.
     * 
     * @param name
     *            the private name
     * @return the private property or {@code null} if not present
     */
    Property get(PrivateName name);

    /**
     * Adds a new private name property.
     * 
     * @param name
     *            the private name
     * @param property
     *            the new private property
     */
    void define(PrivateName name, Property property);
}
