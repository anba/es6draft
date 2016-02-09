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
 * <li>19.5.5 Native Error Types Used in This Standard
 * <li>19.5.6 NativeError Object Structure
 * <ul>
 * <li>19.5.6.1 NativeError Constructors
 * <li>19.5.6.2 Properties of the NativeError Constructors
 * </ul>
 * </ul>
 */
public final class NativeErrorConstructor extends BuiltinConstructor implements Initializable {
    /**
     * 19.5.5 Native Error Types Used in This Standard
     * <ul>
     * <li>19.5.5.1 EvalError
     * <li>19.5.5.2 RangeError
     * <li>19.5.5.3 ReferenceError
     * <li>19.5.5.4 SyntaxError
     * <li>19.5.5.5 TypeError
     * <li>19.5.5.6 URIError
     * </ul>
     */
    public enum ErrorType {
        EvalError, RangeError, ReferenceError, SyntaxError, TypeError, URIError, InternalError;

        private Intrinsics prototype() {
            switch (this) {
            case EvalError:
                return Intrinsics.EvalErrorPrototype;
            case RangeError:
                return Intrinsics.RangeErrorPrototype;
            case ReferenceError:
                return Intrinsics.ReferenceErrorPrototype;
            case SyntaxError:
                return Intrinsics.SyntaxErrorPrototype;
            case TypeError:
                return Intrinsics.TypeErrorPrototype;
            case URIError:
                return Intrinsics.URIErrorPrototype;
            case InternalError:
                return Intrinsics.InternalErrorPrototype;
            default:
                throw new AssertionError();
            }
        }
    }

    private final ErrorType type;

    /**
     * Constructs a new NativeError constructor function.
     * 
     * @param realm
     *            the realm object
     * @param type
     *            the native error type
     */
    public NativeErrorConstructor(Realm realm, ErrorType type) {
        super(realm, type.name(), 1);
        this.type = type;
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, propertiesForType(type));
    }

    @Override
    public NativeErrorConstructor clone() {
        return new NativeErrorConstructor(getRealm(), type);
    }

    /**
     * 19.5.6.1.1 NativeError (message)
     * <p>
     * <strong>Extension</strong>: NativeError (message, fileName, lineNumber, columnNumber)
     */
    @Override
    public ErrorObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* steps 1-5 */
        return construct(callerContext, this, args);
    }

    /**
     * 19.5.6.1.1 NativeError (message)
     * <p>
     * <strong>Extension</strong>: NativeError (message, fileName, lineNumber, columnNumber)
     */
    @Override
    public ErrorObject construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object message = argument(args, 0);
        /* step 1 (not applicable) */
        /* steps 2-3 */
        ErrorObject obj = OrdinaryCreateFromConstructor(calleeContext, newTarget, type.prototype(),
                NativeErrorObjectAllocator.INSTANCE);
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

    private static final class NativeErrorObjectAllocator implements ObjectAllocator<ErrorObject> {
        static final ObjectAllocator<ErrorObject> INSTANCE = new NativeErrorObjectAllocator();

        @Override
        public ErrorObject newInstance(Realm realm) {
            return new ErrorObject(realm);
        }
    }

    private static Class<?> propertiesForType(ErrorType errorType) {
        switch (errorType) {
        case EvalError:
            return EvalErrorConstructorProperties.class;
        case RangeError:
            return RangeErrorConstructorProperties.class;
        case ReferenceError:
            return ReferenceErrorConstructorProperties.class;
        case SyntaxError:
            return SyntaxErrorConstructorProperties.class;
        case TypeError:
            return TypeErrorConstructorProperties.class;
        case URIError:
            return URIErrorConstructorProperties.class;
        case InternalError:
            return InternalErrorConstructorProperties.class;
        default:
            throw new AssertionError();
        }
    }

    /**
     * 19.5.6.2 Properties of the NativeError Constructors
     */
    public enum EvalErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "EvalError";

        /**
         * 19.5.6.2.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.EvalErrorPrototype;
    }

    /**
     * 19.5.6.2 Properties of the NativeError Constructors
     */
    public enum RangeErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "RangeError";

        /**
         * 19.5.6.2.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.RangeErrorPrototype;
    }

    /**
     * 19.5.6.2 Properties of the NativeError Constructors
     */
    public enum ReferenceErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "ReferenceError";

        /**
         * 19.5.6.2.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.ReferenceErrorPrototype;
    }

    /**
     * 19.5.6.2 Properties of the NativeError Constructors
     */
    public enum SyntaxErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "SyntaxError";

        /**
         * 19.5.6.2.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.SyntaxErrorPrototype;
    }

    /**
     * 19.5.6.2 Properties of the NativeError Constructors
     */
    public enum TypeErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "TypeError";

        /**
         * 19.5.6.2.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.TypeErrorPrototype;
    }

    /**
     * 19.5.6.2 Properties of the NativeError Constructors
     */
    public enum URIErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "URIError";

        /**
         * 19.5.6.2.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.URIErrorPrototype;
    }

    /**
     * 19.5.6.2 Properties of the NativeError Constructors
     */
    public enum InternalErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "InternalError";

        /**
         * 19.5.6.2.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.InternalErrorPrototype;
    }
}
