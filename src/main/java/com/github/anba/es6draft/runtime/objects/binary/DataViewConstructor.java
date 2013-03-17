/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToUint32;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.13 Binary Data Objects</h2><br>
 * <h3>15.13.7 DataView Objects</h3>
 * <ul>
 * <li>15.13.7.1 The DataView Constructor Called as a Function
 * <li>15.13.7.2 The DataView Constructor
 * <li>15.13.7.3 Properties of the DataView Constructor
 * </ul>
 */
public class DataViewConstructor extends BuiltinFunction implements Constructor, Initialisable {
    public DataViewConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
        AddRestrictedFunctionProperties(realm, this);
    }

    @Override
    public String toSource() {
        return "function DataView() { /* native code */ }";
    }

    /**
     * 15.13.7.1 The DataView Constructor Called as a Function
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        return construct(args);
    }

    /**
     * 15.13.7.2.1 new DataView(buffer [, byteOffset [, byteLength]])
     */
    @Override
    public Object construct(Object... args) {
        Realm realm = realm();
        Object buffer = args.length > 0 ? args[0] : UNDEFINED;
        ScriptObject obj = ToObject(realm, buffer);
        if (!(obj instanceof ArrayBufferObject)) {
            throwTypeError(realm, Messages.Key.IncompatibleObject);
        }
        long byteOffset = args.length > 1 ? ToUint32(realm, args[1]) : 0;
        long bufferLength = ToUint32(realm, Get(obj, "byteLength"));
        long byteLength = args.length > 2 ? ToUint32(realm, args[2]) : (bufferLength - byteOffset);
        if (byteOffset + byteLength > bufferLength) {
            throwRangeError(realm, Messages.Key.ArrayOffsetOutOfRange);
        }

        DataViewObject view = new DataViewObject(realm);
        view.setPrototype(realm.getIntrinsic(Intrinsics.DataViewPrototype));
        view.defineOwnProperty("byteLength",
                new PropertyDescriptor(byteLength, false, false, false));
        view.defineOwnProperty("buffer", new PropertyDescriptor(obj, false, false, false));
        view.defineOwnProperty("byteOffset",
                new PropertyDescriptor(byteOffset, false, false, false));

        return view;
    }

    /**
     * 15.13.7.3 Properties of the DataView Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 3;

        /**
         * 15.13.7.3.1 DataView.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.DataViewPrototype;
    }
}
