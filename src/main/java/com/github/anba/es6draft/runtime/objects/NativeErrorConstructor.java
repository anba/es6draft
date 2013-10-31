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
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
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
public class NativeErrorConstructor extends BuiltinConstructor implements Initialisable {
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
     * 19.5.6.1.1 NativeError (message)
     * <p>
     * <strong>Extension</strong>: NativeError (message, fileName, lineNumber, columnNumber)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object message = args.length > 0 ? args[0] : UNDEFINED;

        /* step 1 (omitted) */
        /* steps 2-4 */
        ErrorObject obj;
        if (!Type.isObject(thisValue) || !(thisValue instanceof ErrorObject)
                || ((ErrorObject) thisValue).isInitialised()) {
            obj = OrdinaryCreateFromConstructor(calleeContext, this, type.prototype(),
                    NativeErrorObjectAllocator.INSTANCE);
        } else {
            obj = (ErrorObject) thisValue;
        }

        /* step 5 */
        obj.initialise();

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
     * 19.5.6.1.2 new NativeError (...argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
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

        /**
         * 19.5.6.2.2 NativeError [ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.EvalErrorPrototype,
                    NativeErrorObjectAllocator.INSTANCE);
        }
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

        /**
         * 19.5.6.2.2 NativeError [ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.RangeErrorPrototype,
                    NativeErrorObjectAllocator.INSTANCE);
        }
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

        /**
         * 19.5.6.2.2 NativeError [ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.ReferenceErrorPrototype,
                    NativeErrorObjectAllocator.INSTANCE);
        }
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

        /**
         * 19.5.6.2.2 NativeError [ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.SyntaxErrorPrototype,
                    NativeErrorObjectAllocator.INSTANCE);
        }
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

        /**
         * 19.5.6.2.2 NativeError [ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.TypeErrorPrototype,
                    NativeErrorObjectAllocator.INSTANCE);
        }
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

        /**
         * 19.5.6.2.2 NativeError [ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.URIErrorPrototype,
                    NativeErrorObjectAllocator.INSTANCE);
        }
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

        /**
         * 19.5.6.2.2 NativeError [ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
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
