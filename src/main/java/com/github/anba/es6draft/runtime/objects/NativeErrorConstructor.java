/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateOwnDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.OrdinaryCreateFromConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
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
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.11 Error Objects</h2>
 * <ul>
 * <li>15.11.6 Native Error Types Used in This Standard
 * <li>15.11.7 NativeError Object Structure
 * <ul>
 * <li>15.11.7.1 NativeError Constructors Called as Functions
 * <li>15.11.7.2 The NativeError Constructors
 * <li>15.11.7.3 Properties of the NativeError Constructors
 * </ul>
 * </ul>
 */
public class NativeErrorConstructor extends BuiltinFunction implements Constructor, Initialisable {
    /**
     * 15.11.6 Native Error Types Used in This Standard
     * <ul>
     * <li>15.11.6.1 EvalError
     * <li>15.11.6.2 RangeError
     * <li>15.11.6.3 ReferenceError
     * <li>15.11.6.4 SyntaxError
     * <li>15.11.6.5 TypeError
     * <li>15.11.6.6 URIError
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
                throw new IllegalStateException();
            }
        }
    }

    private final ErrorType type;

    public NativeErrorConstructor(Realm realm, ErrorType type) {
        super(realm);
        this.type = type;
    }

    @Override
    public void initialise(ExecutionContext cx) {
        switch (type) {
        case EvalError:
            createProperties(this, cx, EvalErrorConstructorProperties.class);
            break;
        case RangeError:
            createProperties(this, cx, RangeErrorConstructorProperties.class);
            break;
        case ReferenceError:
            createProperties(this, cx, ReferenceErrorConstructorProperties.class);
            break;
        case SyntaxError:
            createProperties(this, cx, SyntaxErrorConstructorProperties.class);
            break;
        case TypeError:
            createProperties(this, cx, TypeErrorConstructorProperties.class);
            break;
        case URIError:
            createProperties(this, cx, URIErrorConstructorProperties.class);
            break;
        case InternalError:
            createProperties(this, cx, InternalErrorConstructorProperties.class);
            break;
        default:
            throw new IllegalStateException();
        }
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 15.11.7.1.1 NativeError (message)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        Object message = args.length > 0 ? args[0] : UNDEFINED;

        ErrorObject obj;
        if (!Type.isObject(thisValue) || !(thisValue instanceof ErrorObject)
                || ((ErrorObject) thisValue).isInitialised()) {
            obj = OrdinaryCreateFromConstructor(callerContext, this, type.prototype(),
                    NativeErrorObjectAllocator.INSTANCE);
        } else {
            obj = (ErrorObject) thisValue;
        }

        obj.initialise();

        if (!Type.isUndefined(message)) {
            CharSequence msg = ToString(callerContext, message);
            CreateOwnDataProperty(callerContext, obj, "message", msg);
        }

        return obj;
    }

    /**
     * 15.11.7.2.1 new NativeError (... args)
     */
    @Override
    public Object construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * 15.11.7.3 Properties of the NativeError Constructors
     */
    public enum EvalErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "EvalError";

        /**
         * 15.11.7.3.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.EvalErrorPrototype;

        /**
         * 15.11.7.3.2 NativeError [ @@create ] ( )
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.EvalErrorPrototype,
                    NativeErrorObjectAllocator.INSTANCE);
        }
    }

    /**
     * 15.11.7.3 Properties of the NativeError Constructors
     */
    public enum RangeErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "RangeError";

        /**
         * 15.11.7.3.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.RangeErrorPrototype;

        /**
         * 15.11.7.3.2 NativeError [ @@create ] ( )
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.RangeErrorPrototype,
                    NativeErrorObjectAllocator.INSTANCE);
        }
    }

    /**
     * 15.11.7.3 Properties of the NativeError Constructors
     */
    public enum ReferenceErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "ReferenceError";

        /**
         * 15.11.7.3.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.ReferenceErrorPrototype;

        /**
         * 15.11.7.3.2 NativeError [ @@create ] ( )
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.ReferenceErrorPrototype,
                    NativeErrorObjectAllocator.INSTANCE);
        }
    }

    /**
     * 15.11.7.3 Properties of the NativeError Constructors
     */
    public enum SyntaxErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "SyntaxError";

        /**
         * 15.11.7.3.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.SyntaxErrorPrototype;

        /**
         * 15.11.7.3.2 NativeError [ @@create ] ( )
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.SyntaxErrorPrototype,
                    NativeErrorObjectAllocator.INSTANCE);
        }
    }

    /**
     * 15.11.7.3 Properties of the NativeError Constructors
     */
    public enum TypeErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "TypeError";

        /**
         * 15.11.7.3.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.TypeErrorPrototype;

        /**
         * 15.11.7.3.2 NativeError [ @@create ] ( )
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.TypeErrorPrototype,
                    NativeErrorObjectAllocator.INSTANCE);
        }
    }

    /**
     * 15.11.7.3 Properties of the NativeError Constructors
     */
    public enum URIErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "URIError";

        /**
         * 15.11.7.3.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.URIErrorPrototype;

        /**
         * 15.11.7.3.2 NativeError [ @@create ] ( )
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.URIErrorPrototype,
                    NativeErrorObjectAllocator.INSTANCE);
        }
    }

    /**
     * 15.11.7.3 Properties of the NativeError Constructors
     */
    public enum InternalErrorConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.Error;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "InternalError";

        /**
         * 15.11.7.3.1 NativeError.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.InternalErrorPrototype;

        /**
         * 15.11.7.3.2 NativeError [ @@create ] ( )
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.InternalErrorPrototype,
                    NativeErrorObjectAllocator.INSTANCE);
        }
    }

    private static class NativeErrorObjectAllocator implements ObjectAllocator<ErrorObject> {
        static final ObjectAllocator<ErrorObject> INSTANCE = new NativeErrorObjectAllocator();

        @Override
        public ErrorObject newInstance(Realm realm) {
            return new ErrorObject(realm);
        }
    }
}
