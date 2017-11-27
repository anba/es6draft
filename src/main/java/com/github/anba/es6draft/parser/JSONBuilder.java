/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

/**
 * 
 */
public interface JSONBuilder<DOCUMENT, OBJECT, ARRAY, VALUE> {
    /**
     * Creates a new JSON document.
     * 
     * @param value
     *            the JSON text value
     * @return the new document
     */
    DOCUMENT createDocument(VALUE value);

    /**
     * Starts a new JSON object.
     * 
     * @return the new object
     */
    OBJECT newObject();

    /**
     * Finishes a JSON object.
     * 
     * @param object
     *            the object
     * @return the new object
     */
    VALUE finishObject(OBJECT object);

    /**
     * Creates a new JSON property.
     * 
     * @param object
     *            the JSON object
     * @param name
     *            the property name
     * @param rawName
     *            the raw property name
     * @param index
     *            the property index
     */
    void newProperty(OBJECT object, String name, String rawName, long index);

    /**
     * Finishes a JSON property.
     * 
     * @param object
     *            the JSON object
     * @param name
     *            the property name
     * @param rawName
     *            the raw property name
     * @param index
     *            the property index
     * @param value
     *            the property value
     */
    void finishProperty(OBJECT object, String name, String rawName, long index, VALUE value);

    /**
     * Starts a new JSON array.
     * 
     * @return the new array
     */
    ARRAY newArray();

    /**
     * Finishes a JSON array.
     * 
     * @param array
     *            the array
     * @return the new array
     */
    VALUE finishArray(ARRAY array);

    /**
     * Creates a new JSON element.
     * 
     * @param array
     *            the JSON array
     * @param index
     *            the element index
     */
    void newElement(ARRAY array, long index);

    /**
     * Finishes a JSON element.
     * 
     * @param array
     *            the JSON array
     * @param index
     *            the element index
     * @param value
     *            the element value
     */
    void finishElement(ARRAY array, long index, VALUE value);

    /**
     * Creates a new JSON null value.
     * 
     * @return the null value
     */
    VALUE newNull();

    /**
     * Creates a new JSON boolean value.
     * 
     * @param value
     *            the value
     * @return the boolean value
     */
    VALUE newBoolean(boolean value);

    /**
     * Creates a new JSON number value.
     * 
     * @param value
     *            the value
     * @param rawValue
     *            the raw value
     * @return the number value
     */
    VALUE newNumber(double value, String rawValue);

    /**
     * Creates a new JSON string value.
     * 
     * @param value
     *            the value
     * @param rawValue
     *            the raw value
     * @return the string value
     */
    VALUE newString(String value, String rawValue);
}
