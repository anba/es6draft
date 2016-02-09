/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataPropertyOrThrow;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 *
 */
public final class JSONObjectBuilder implements
        JSONBuilder<Object, OrdinaryObject, ArrayObject, Object> {
    private final ExecutionContext cx;

    public JSONObjectBuilder(ExecutionContext cx) {
        this.cx = cx;
    }

    @Override
    public Object createDocument(Object value) {
        return value;
    }

    @Override
    public OrdinaryObject newObject() {
        return ObjectCreate(cx, Intrinsics.ObjectPrototype);
    }

    @Override
    public Object finishObject(OrdinaryObject object) {
        return object;
    }

    @Override
    public void newProperty(OrdinaryObject object, String name, String rawName, long index) {
        // empty
    }

    @Override
    public void finishProperty(OrdinaryObject object, String name, String rawName, long index,
            Object value) {
        CreateDataPropertyOrThrow(cx, object, name, value);
    }

    @Override
    public ArrayObject newArray() {
        return ArrayCreate(cx, 0);
    }

    @Override
    public Object finishArray(ArrayObject array) {
        return array;
    }

    @Override
    public void newElement(ArrayObject array, long index) {
        // empty
    }

    @Override
    public void finishElement(ArrayObject array, long index, Object value) {
        CreateDataPropertyOrThrow(cx, array, index, value);
    }

    @Override
    public Object newNull() {
        return NULL;
    }

    @Override
    public Object newBoolean(boolean value) {
        return value;
    }

    @Override
    public Object newNumber(double value, String rawValue) {
        return value;
    }

    @Override
    public Object newString(String value, String rawValue) {
        return value;
    }
}
