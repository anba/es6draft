/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.11 Error Objects</h2>
 * <ul>
 * <li>15.11.1 The Error Constructor Called as a Function
 * <li>15.11.2 The Error Constructor
 * <li>15.11.3 Properties of the Error Constructor
 * </ul>
 */
public class ErrorConstructor extends BuiltinFunction implements Constructor, Initialisable {
    public ErrorConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 15.11.1.1 Error (message)
     * <p>
     * <strong>Extension</strong>: Error (message, fileName, lineNumber)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object message = args.length > 0 ? args[0] : UNDEFINED;

        ErrorObject obj;
        if (!Type.isObject(thisValue) || !(thisValue instanceof ErrorObject)
                || ((ErrorObject) thisValue).isInitialised()) {
            obj = OrdinaryCreateFromConstructor(calleeContext, this, Intrinsics.ErrorPrototype,
                    ErrorObjectAllocator.INSTANCE);
        } else {
            obj = (ErrorObject) thisValue;
        }

        obj.initialise();

        if (!Type.isUndefined(message)) {
            CharSequence msg = ToString(calleeContext, message);
            PropertyDescriptor msgDesc = new PropertyDescriptor(msg, true, false, true);
            DefinePropertyOrThrow(calleeContext, obj, "message", msgDesc);
        }
        if (args.length > 1) {
            CharSequence fileName = ToString(calleeContext, args[1]);
            CreateOwnDataProperty(calleeContext, obj, "fileName", fileName);
        }
        if (args.length > 2) {
            int lineNumber = ToInt32(calleeContext, args[2]);
            CreateOwnDataProperty(calleeContext, obj, "lineNumber", lineNumber);
        }

        return obj;
    }

    /**
     * 15.11.2.1 new Error (message)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * 15.11.3 Properties of the Error Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "Error";

        /**
         * 15.11.3.1 Error.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.ErrorPrototype;

        /**
         * 15.11.3.2 Error[ @@create ] ( )
         */
        @Function(name = "@@create", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.ErrorPrototype,
                    ErrorObjectAllocator.INSTANCE);
        }
    }

    private static class ErrorObjectAllocator implements ObjectAllocator<ErrorObject> {
        static final ObjectAllocator<ErrorObject> INSTANCE = new ErrorObjectAllocator();

        @Override
        public ErrorObject newInstance(Realm realm) {
            return new ErrorObject(realm);
        }
    }
}
