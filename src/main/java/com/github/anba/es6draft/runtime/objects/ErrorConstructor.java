/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Creatable;
import com.github.anba.es6draft.runtime.types.CreateAction;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.5 Error Objects</h2>
 * <ul>
 * <li>19.5.1 The Error Constructor
 * <li>19.5.2 Properties of the Error Constructor
 * </ul>
 */
public final class ErrorConstructor extends BuiltinConstructor implements Initializable,
        Creatable<ErrorObject> {
    /**
     * Constructs a new Error constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public ErrorConstructor(Realm realm) {
        super(realm, "Error", 1);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    @Override
    public ErrorConstructor clone() {
        return new ErrorConstructor(getRealm());
    }

    /**
     * 19.5.1.1 Error (message)
     * <p>
     * <strong>Extension</strong>: Error (message, fileName, lineNumber, columnNumber)
     */
    @Override
    public ErrorObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object message = argument(args, 0);

        /* step 1 (omitted) */
        /* steps 2-4 */
        ErrorObject obj;
        if (!(thisValue instanceof ErrorObject) || ((ErrorObject) thisValue).isInitialized()) {
            obj = OrdinaryCreateFromConstructor(calleeContext, this, Intrinsics.ErrorPrototype,
                    ErrorObjectAllocator.INSTANCE);
        } else {
            obj = (ErrorObject) thisValue;
        }

        /* step 5 */
        obj.initialize();

        /* step 6 */
        if (!Type.isUndefined(message)) {
            CharSequence msg = ToString(calleeContext, message);
            PropertyDescriptor msgDesc = new PropertyDescriptor(msg, true, false, true);
            DefinePropertyOrThrow(calleeContext, obj, "message", msgDesc);
        }

        /* extension: fileName, lineNumber and columnNumber arguments */
        if (args.length > 1) {
            CharSequence fileName = ToString(calleeContext, args[1]);
            CreateDataProperty(calleeContext, obj, "fileName", fileName);
        }
        if (args.length > 2) {
            int lineNumber = ToInt32(calleeContext, args[2]);
            CreateDataProperty(calleeContext, obj, "lineNumber", lineNumber);
        }
        if (args.length > 3) {
            int columnNumber = ToInt32(calleeContext, args[3]);
            CreateDataProperty(calleeContext, obj, "columnNumber", columnNumber);
        }

        /* step 7 */
        return obj;
    }

    /**
     * 19.5.1.2 new Error(...argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    @Override
    public CreateAction<ErrorObject> createAction() {
        return ErrorCreate.INSTANCE;
    }

    /**
     * 19.5.2 Properties of the Error Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "Error";

        /**
         * 19.5.2.1 Error.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.ErrorPrototype;
    }

    private static final class ErrorObjectAllocator implements ObjectAllocator<ErrorObject> {
        static final ObjectAllocator<ErrorObject> INSTANCE = new ErrorObjectAllocator();

        @Override
        public ErrorObject newInstance(Realm realm) {
            return new ErrorObject(realm);
        }
    }

    private static final class ErrorCreate implements CreateAction<ErrorObject> {
        static final CreateAction<ErrorObject> INSTANCE = new ErrorCreate();

        @Override
        public ErrorObject create(ExecutionContext cx, Constructor constructor, Object... args) {
            return OrdinaryCreateFromConstructor(cx, constructor, Intrinsics.ErrorPrototype,
                    ErrorObjectAllocator.INSTANCE);
        }
    }
}
