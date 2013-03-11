/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.11 Error Objects</h2>
 * <ul>
 * <li>15.11.6 Native Error Types Used in This Standard
 * <li>15.11.7 NativeError Object Structure
 * <ul>
 * <li>15.11.7.1 NativeError Constructors Called as Functions
 * <li>15.11.7.2 NativeError (message)
 * <li>15.11.7.3 The NativeError Constructors
 * <li>15.11.7.4 new NativeError (message)
 * <li>15.11.7.5 Properties of the NativeError Constructors
 * <li>15.11.7.6 Properties of the NativeError Prototype Objects
 * <li>15.11.7.8 Properties of NativeError Instances
 * </ul>
 * </ul>
 */
public class NativeError extends OrdinaryObject implements Scriptable, Callable, Constructor {
    private final ErrorType type;
    private final Scriptable proto;

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
        EvalError, RangeError, ReferenceError, SyntaxError, TypeError, URIError, InternalError
    }

    public NativeError(Realm realm, ErrorType type, Scriptable proto) {
        super(realm);
        this.type = type;
        this.proto = proto;
    }

    @Override
    public String toString() {
        return type.name();
    }

    private static class NativeErrorPrototype extends OrdinaryObject {
        public NativeErrorPrototype(Realm realm) {
            super(realm);
        }
    }

    public static NativeError create(Realm realm, ErrorType type) {
        NativeErrorPrototype proto = new NativeErrorPrototype(realm);
        NativeError ctor = new NativeError(realm, type, proto);

        createProperties(ctor, realm, ConstructorProperties.class);
        // 15.11.7.5.1 NativeError.prototype
        ctor.defineOwnProperty("prototype", new PropertyDescriptor(proto, false, false, false));

        createProperties(proto, realm, PrototypeProperties.class);
        // 15.11.7.6.1 NativeError.prototype.constructor
        proto.defineOwnProperty("constructor", new PropertyDescriptor(ctor, true, false, true));
        // 15.11.7.6.2 NativeError.prototype.name
        proto.defineOwnProperty("name", new PropertyDescriptor(type.name(), true, false, true));

        return ctor;
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinFunction;
    }

    @Override
    public String toSource() {
        return String.format("function %s() { /* native code */ }", type.name());
    }

    /**
     * 15.11.7.2 NativeError (message)
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        Object message = args.length > 0 ? args[0] : UNDEFINED;
        ErrorObject obj = new ErrorObject(realm());
        obj.setPrototype(proto);
        if (!Type.isUndefined(message)) {
            CharSequence msg = ToString(realm(), message);
            obj.defineOwnProperty("message", new PropertyDescriptor(msg, true, false, true));
        }
        return obj;
    }

    /**
     * 15.11.7.4 new NativeError (message)
     */
    @Override
    public Object construct(Object... args) {
        Object message = args.length > 0 ? args[0] : UNDEFINED;
        ErrorObject obj = new ErrorObject(realm());
        obj.setPrototype(proto);
        if (!Type.isUndefined(message)) {
            CharSequence msg = ToString(realm(), message);
            obj.defineOwnProperty("message", new PropertyDescriptor(msg, true, false, true));
        }
        return obj;
    }

    /**
     * 15.11.7.5 Properties of the NativeError Constructors
     */
    public enum ConstructorProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;
    }

    /**
     * 15.11.7.6 Properties of the NativeError Prototype Objects
     */
    public enum PrototypeProperties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ErrorPrototype;

        /**
         * 15.11.7.6.3 NativeError.prototype.message
         */
        @Value(name = "message")
        public static final String message = "";
    }
}
