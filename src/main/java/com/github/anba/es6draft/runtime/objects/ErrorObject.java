/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import java.util.Objects;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.5 Error Objects</h2>
 * <ul>
 * <li>19.5.4 Properties of Error Instances
 * <li>19.5.6 NativeError Object Structure
 * <ul>
 * <li>19.5.6.4 Properties of NativeError Instances
 * </ul>
 * </ul>
 */
public final class ErrorObject extends OrdinaryObject {
    private final ScriptException exception;

    /**
     * Constructs a new Error object.
     * 
     * @param realm
     *            the realm object
     */
    public ErrorObject(Realm realm) {
        super(realm);
        this.exception = new ScriptException(this);
    }

    /**
     * Constructs a new Error object.
     * 
     * @param realm
     *            the realm object
     * @param cause
     *            the exception's cause
     */
    public ErrorObject(Realm realm, Throwable cause) {
        super(realm);
        this.exception = new ScriptException(this, cause);
    }

    /**
     * Constructs a new Error object.
     * 
     * @param realm
     *            the realm object
     * @param prototype
     *            the error prototype
     * @param message
     *            the error message
     */
    public ErrorObject(Realm realm, Intrinsics prototype, String message) {
        this(realm);
        setPrototype(realm.getIntrinsic(prototype));
        defineErrorProperty("message", message, false);
    }

    /**
     * Constructs a new Error object.
     * 
     * @param realm
     *            the realm object
     * @param cause
     *            the exception's cause
     * @param prototype
     *            the error prototype
     * @param message
     *            the error message
     */
    public ErrorObject(Realm realm, Throwable cause, Intrinsics prototype, String message) {
        this(realm, cause);
        setPrototype(realm.getIntrinsic(prototype));
        defineErrorProperty("message", message, false);
    }

    /**
     * Constructs a new Error object.
     * 
     * @param realm
     *            the realm object
     * @param prototype
     *            the error prototype
     * @param message
     *            the error message
     * @param fileName
     *            the file name
     * @param lineNumber
     *            the line number
     * @param columnNumber
     *            the column number
     */
    public ErrorObject(Realm realm, Intrinsics prototype, String message, String fileName,
            int lineNumber, int columnNumber) {
        this(realm);
        setPrototype(realm.getIntrinsic(prototype));
        defineErrorProperty("message", message, false);
        defineErrorProperty("fileName", fileName, true);
        defineErrorProperty("lineNumber", lineNumber, true);
        defineErrorProperty("columnNumber", columnNumber, true);
    }

    /**
     * Constructs a new Error object.
     * 
     * @param realm
     *            the realm object
     * @param cause
     *            the exception's cause
     * @param prototype
     *            the error prototype
     * @param message
     *            the error message
     * @param fileName
     *            the file name
     * @param lineNumber
     *            the line number
     * @param columnNumber
     *            the column number
     */
    public ErrorObject(Realm realm, Throwable cause, Intrinsics prototype, String message,
            String fileName, int lineNumber, int columnNumber) {
        this(realm, cause);
        setPrototype(realm.getIntrinsic(prototype));
        defineErrorProperty("message", message, false);
        defineErrorProperty("fileName", fileName, true);
        defineErrorProperty("lineNumber", lineNumber, true);
        defineErrorProperty("columnNumber", columnNumber, true);
    }

    /*package*/void defineErrorProperty(String name, Object value, boolean enumerable) {
        infallibleDefineOwnProperty(name, new Property(value, true, enumerable, true));
    }

    /**
     * Returns the wrapped {@link ScriptException} object.
     * 
     * @return the wrapped {@link ScriptException} object
     */
    public ScriptException getException() {
        return exception;
    }

    @Override
    public String className() {
        return "Error";
    }

    @Override
    public String toString() {
        String name = getErrorObjectProperty(this, "name", "Error");
        String message = getErrorObjectProperty(this, "message", "");
        if (name.length() == 0) {
            return message;
        }
        if (message.length() == 0) {
            return name;
        }
        return name + ": " + message;
    }

    /**
     * Specialized property retrieval to prevent any script execution.
     * 
     * @param error
     *            the error object
     * @param propertyName
     *            the property key
     * @param defaultValue
     *            the default value
     * @return property string value
     */
    private static String getErrorObjectProperty(ErrorObject error, String propertyName, String defaultValue) {
        Property property = error.lookupOwnProperty(propertyName);
        if (property == null) {
            ScriptObject proto = error.getPrototype();
            if (proto instanceof ErrorPrototype || proto instanceof NativeErrorPrototype) {
                property = ((OrdinaryObject) proto).lookupOwnProperty(propertyName);
            }
        }
        Object value = property != null && property.isDataDescriptor() ? property.getValue() : null;
        if (value == null || Type.isUndefined(value)) {
            return defaultValue;
        }
        // Prevent possible recursion
        if (value instanceof ErrorObject) {
            return "<error>";
        }
        return Objects.toString(value);
    }
}
