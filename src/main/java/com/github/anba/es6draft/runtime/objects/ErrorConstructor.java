/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToInt32;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
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
public final class ErrorConstructor extends BuiltinConstructor implements Initializable {
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
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
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
        /* steps 1-5 */
        return construct(callerContext, this, args);
    }

    /**
     * 19.5.1.1 Error (message)
     * <p>
     * <strong>Extension</strong>: Error (message, fileName, lineNumber, columnNumber)
     */
    @Override
    public ErrorObject construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object message = argument(args, 0);
        /* step 1 (not applicable) */
        /* steps 2-3 */
        ErrorObject obj = OrdinaryCreateFromConstructor(calleeContext, newTarget,
                Intrinsics.ErrorPrototype, ErrorObjectAllocator.INSTANCE);
        /* step 4 */
        if (!Type.isUndefined(message)) {
            CharSequence msg = ToString(calleeContext, message);
            obj.defineErrorProperty("message", msg, false);
        }

        /* extension: fileName, lineNumber and columnNumber arguments */
        if (args.length > 1) {
            CharSequence fileName = ToString(calleeContext, args[1]);
            obj.defineErrorProperty("fileName", fileName, true);
        }
        if (args.length > 2) {
            int line = ToInt32(calleeContext, args[2]);
            obj.defineErrorProperty("lineNumber", line, true);
        }
        if (args.length > 3) {
            int column = ToInt32(calleeContext, args[3]);
            obj.defineErrorProperty("columnNumber", column, true);
        }

        /* step 5 */
        return obj;
    }

    private static final class ErrorObjectAllocator implements ObjectAllocator<ErrorObject> {
        static final ObjectAllocator<ErrorObject> INSTANCE = new ErrorObjectAllocator();

        @Override
        public ErrorObject newInstance(Realm realm) {
            return new ErrorObject(realm);
        }
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
}
