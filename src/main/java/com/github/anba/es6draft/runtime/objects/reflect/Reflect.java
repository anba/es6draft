/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.PrepareForTailCall;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.FromPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.ToPropertyDescriptor;

import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.TailCall;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>26 Reflection</h1>
 * <ul>
 * <li>26.1 The Reflect Object
 * </ul>
 */
public final class Reflect extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Reflect object.
     * 
     * @param realm
     *            the realm object
     */
    public Reflect(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
        createProperties(cx, this, RealmProperty.class);
        createProperties(cx, this, LoaderProperty.class);
        createProperties(cx, this, ParseProperty.class);
    }

    /**
     * 26.1 Properties of the Reflect Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 26.1.1 Reflect.apply ( target, thisArgument, argumentsList )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @param thisArgument
         *            the this-binding for the [[Call]] invocation
         * @param argumentsList
         *            the function arguments
         * @return the function call result
         */
        @TailCall
        @Function(name = "apply", arity = 3)
        public static Object apply(ExecutionContext cx, Object thisValue, Object target,
                Object thisArgument, Object argumentsList) {
            /* step 1 */
            if (!IsCallable(target)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            /* steps 2-3 */
            Object[] args = CreateListFromArrayLike(cx, argumentsList);
            /* steps 4-5 */
            return PrepareForTailCall(args, thisArgument, (Callable) target);
        }

        /**
         * 26.1.2 Reflect.construct ( target, argumentsList )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @param argumentsList
         *            the function arguments
         * @return the new script object
         */
        @Function(name = "construct", arity = 2)
        public static Object construct(ExecutionContext cx, Object thisValue, Object target,
                Object argumentsList) {
            /* step 1 */
            if (!IsConstructor(target)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            /* steps 2-3 */
            Object[] args = CreateListFromArrayLike(cx, argumentsList);
            /* steps 4-5 */
            return ((Constructor) target).construct(cx, args);
        }

        /**
         * 26.1.8 Reflect.getPrototypeOf (target)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @return the prototype object
         */
        @Function(name = "getPrototypeOf", arity = 1)
        public static Object getPrototypeOf(ExecutionContext cx, Object thisValue, Object target) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* step 3 */
            ScriptObject proto = obj.getPrototypeOf(cx);
            return proto != null ? proto : NULL;
        }

        /**
         * 26.1.14 Reflect.setPrototypeOf (target, proto)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @param proto
         *            the new prototype object
         * @return {@code true} on success
         */
        @Function(name = "setPrototypeOf", arity = 2)
        public static Object setPrototypeOf(ExecutionContext cx, Object thisValue, Object target,
                Object proto) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* step 3 */
            if (!Type.isObjectOrNull(proto)) {
                throw newTypeError(cx, Messages.Key.NotObjectOrNull);
            }
            /* step 4 */
            return obj.setPrototypeOf(cx, Type.objectValueOrNull(proto));
        }

        /**
         * 26.1.10 Reflect.isExtensible (target)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @return {@code true} if the object is extensible
         */
        @Function(name = "isExtensible", arity = 1)
        public static Object isExtensible(ExecutionContext cx, Object thisValue, Object target) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* step 3 */
            return obj.isExtensible(cx);
        }

        /**
         * 26.1.12 Reflect.preventExtensions (target)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @return {@code true} on success
         */
        @Function(name = "preventExtensions", arity = 1)
        public static Object preventExtensions(ExecutionContext cx, Object thisValue, Object target) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* step 3 */
            return obj.preventExtensions(cx);
        }

        /**
         * 26.1.9 Reflect.has (target, propertyKey)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @param propertyKey
         *            the property key
         * @return {@code true} if the property was found
         */
        @Function(name = "has", arity = 2)
        public static Object has(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* steps 3-4 */
            Object key = ToPropertyKey(cx, propertyKey);
            /* step 5 */
            if (key instanceof String) {
                return obj.hasProperty(cx, (String) key);
            } else {
                assert key instanceof Symbol;
                return obj.hasProperty(cx, (Symbol) key);
            }
        }

        /**
         * 26.1.7 Reflect.getOwnPropertyDescriptor(target, propertyKey)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @param propertyKey
         *            the property key
         * @return the property descriptor object
         */
        @Function(name = "getOwnPropertyDescriptor", arity = 2)
        public static Object getOwnPropertyDescriptor(ExecutionContext cx, Object thisValue,
                Object target, Object propertyKey) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* steps 3-4 */
            Object key = ToPropertyKey(cx, propertyKey);
            /* step 5 */
            Property desc;
            if (key instanceof String) {
                desc = obj.getOwnProperty(cx, (String) key);
            } else {
                assert key instanceof Symbol;
                desc = obj.getOwnProperty(cx, (Symbol) key);
            }
            /* step 6 */
            return FromPropertyDescriptor(cx, desc);
        }

        /**
         * 26.1.6 Reflect.get (target, propertyKey [, receiver ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @param propertyKey
         *            the property key
         * @param receiver
         *            the optional receiver object
         * @return the property value
         */
        @Function(name = "get", arity = 3)
        public static Object get(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey, @Optional(Optional.Default.NONE) Object receiver) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* steps 3-4 */
            Object key = ToPropertyKey(cx, propertyKey);
            /* step 5 */
            if (receiver == null) {
                receiver = target;
            }
            /* step 6 */
            if (key instanceof String) {
                return obj.get(cx, (String) key, receiver);
            } else {
                assert key instanceof Symbol;
                return obj.get(cx, (Symbol) key, receiver);
            }
        }

        /**
         * 26.1.13 Reflect.set (target, propertyKey, V [, receiver ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @param propertyKey
         *            the property key
         * @param value
         *            the new property value
         * @param receiver
         *            the optional receiver object
         * @return {@code true} on success
         */
        @Function(name = "set", arity = 4)
        public static Object set(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey, Object value, @Optional(Optional.Default.NONE) Object receiver) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* steps 3-4 */
            Object key = ToPropertyKey(cx, propertyKey);
            /* step 5 */
            if (receiver == null) {
                receiver = target;
            }
            /* step 6 */
            if (key instanceof String) {
                return obj.set(cx, (String) key, value, receiver);
            } else {
                assert key instanceof Symbol;
                return obj.set(cx, (Symbol) key, value, receiver);
            }
        }

        /**
         * 26.1.4 Reflect.deleteProperty (target, propertyKey)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @param propertyKey
         *            the property key
         * @return {@code true} on success
         */
        @Function(name = "deleteProperty", arity = 2)
        public static Object deleteProperty(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* steps 3-4 */
            Object key = ToPropertyKey(cx, propertyKey);
            /* step 5 */
            if (key instanceof String) {
                return obj.delete(cx, (String) key);
            } else {
                assert key instanceof Symbol;
                return obj.delete(cx, (Symbol) key);
            }
        }

        /**
         * 26.1.3 Reflect.defineProperty(target, propertyKey, attributes)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @param propertyKey
         *            the property key
         * @param attributes
         *            the property descriptor object
         * @return {@code true} on success
         */
        @Function(name = "defineProperty", arity = 3)
        public static Object defineProperty(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey, Object attributes) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* steps 3-4 */
            Object key = ToPropertyKey(cx, propertyKey);
            /* steps 5-6 */
            PropertyDescriptor desc = ToPropertyDescriptor(cx, attributes);
            /* step 7 */
            if (key instanceof String) {
                return obj.defineOwnProperty(cx, (String) key, desc);
            } else {
                assert key instanceof Symbol;
                return obj.defineOwnProperty(cx, (Symbol) key, desc);
            }
        }

        /**
         * 26.1.5 Reflect.enumerate (target)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @return the enumeration iterator object
         */
        @Function(name = "enumerate", arity = 1)
        public static Object enumerate(ExecutionContext cx, Object thisValue, Object target) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* steps 3-4 */
            return obj.enumerate(cx);
        }

        /**
         * 26.1.11 Reflect.ownKeys (target)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @return the properties iterator object
         */
        @Function(name = "ownKeys", arity = 1)
        public static Object ownKeys(ExecutionContext cx, Object thisValue, Object target) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* steps 3-4 */
            List<?> keys = obj.ownPropertyKeys(cx);
            /* step 5 */
            return CreateArrayFromList(cx, keys);
        }
    }

    @CompatibilityExtension(CompatibilityOption.ReflectParse)
    public enum ParseProperty {
        ;

        /**
         * Reflect.parse(src[, options])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param src
         *            the source string
         * @param options
         *            the optional parse options
         * @return the parsed AST object
         */
        @Function(name = "parse", arity = 1)
        public static Object parse(ExecutionContext cx, Object thisValue, Object src, Object options) {
            String source = ToFlatString(cx, src);
            ScriptObject opts;
            if (Type.isUndefinedOrNull(options)) {
                opts = null;
            } else if (!Type.isObject(options)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            } else {
                opts = Type.objectValue(options);
            }
            return ReflectParser.parse(cx, source, opts);
        }
    }

    @CompatibilityExtension(CompatibilityOption.Realm)
    public enum RealmProperty {
        ;

        /**
         * Realm ( . . . )
         */
        @Value(name = "Realm")
        public static final Intrinsics Realm = Intrinsics.Realm;
    }

    @CompatibilityExtension(CompatibilityOption.Loader)
    public enum LoaderProperty {
        ;

        /**
         * Loader ( . . . )
         */
        @Value(name = "Loader")
        public static final Intrinsics Loader = Intrinsics.Loader;
    }
}
