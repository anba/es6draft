/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.language.CallOperations.PrepareForTailCall;
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
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>26 Reflection</h1>
 * <ul>
 * <li>26.1 The Reflect Object
 * </ul>
 */
public final class ReflectObject extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Reflect object.
     * 
     * @param realm
     *            the realm object
     */
    public ReflectObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        if (realm.getRuntimeContext().isEnabled(CompatibilityOption.Realm)
                || realm.getRuntimeContext().isEnabled(CompatibilityOption.FrozenRealm)) {
            createProperties(realm, this, RealmProperty.class);
        }
        createProperties(realm, this, LoaderProperty.class);
        createProperties(realm, this, ParseProperty.class);
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
        public static Object apply(ExecutionContext cx, Object thisValue, Object target, Object thisArgument,
                Object argumentsList) {
            /* step 1 */
            if (!IsCallable(target)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            /* step 2 */
            Object[] args = CreateListFromArrayLike(cx, argumentsList);
            /* steps 3-4 */
            return PrepareForTailCall((Callable) target, thisArgument, args);
        }

        /**
         * 26.1.2 Reflect.construct ( target, argumentsList [, newTarget] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target object
         * @param argumentsList
         *            the function arguments
         * @param newTarget
         *            the newTarget object
         * @return the new script object
         */
        @Function(name = "construct", arity = 2)
        public static Object construct(ExecutionContext cx, Object thisValue, Object target, Object argumentsList,
                @Optional(Optional.Default.NONE) Object newTarget) {
            /* step 1 */
            if (!IsConstructor(target)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            /* steps 2-3 */
            if (newTarget == null) {
                // FIXME: spec issue - compare to undefined?
                // https://bugs.ecmascript.org/show_bug.cgi?id=4416
                newTarget = target;
            } else if (!IsConstructor(newTarget)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            /* step 4 */
            Object[] args = CreateListFromArrayLike(cx, argumentsList);
            /* step 5 */
            return ((Constructor) target).construct(cx, (Constructor) newTarget, args);
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
        public static Object defineProperty(ExecutionContext cx, Object thisValue, Object target, Object propertyKey,
                Object attributes) {
            /* step 1 */
            if (!Type.isObject(target)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject targetObject = Type.objectValue(target);
            /* step 2 */
            Object key = ToPropertyKey(cx, propertyKey);
            /* step 3 */
            PropertyDescriptor desc = ToPropertyDescriptor(cx, attributes);
            /* step 4 */
            return targetObject.defineOwnProperty(cx, key, desc);
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
        public static Object deleteProperty(ExecutionContext cx, Object thisValue, Object target, Object propertyKey) {
            /* step 1 */
            if (!Type.isObject(target)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject targetObject = Type.objectValue(target);
            /* step 2 */
            Object key = ToPropertyKey(cx, propertyKey);
            /* step 3 */
            return targetObject.delete(cx, key);
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
        @Function(name = "get", arity = 2)
        public static Object get(ExecutionContext cx, Object thisValue, Object target, Object propertyKey,
                @Optional(Optional.Default.NONE) Object receiver) {
            /* step 1 */
            if (!Type.isObject(target)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject targetObject = Type.objectValue(target);
            /* step 3 */
            Object key = ToPropertyKey(cx, propertyKey);
            /* step 4 */
            if (receiver == null) {
                receiver = target;
            }
            /* step 5 */
            return targetObject.get(cx, key, receiver);
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
        public static Object getOwnPropertyDescriptor(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey) {
            /* step 1 */
            if (!Type.isObject(target)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject targetObject = Type.objectValue(target);
            /* step 2 */
            Object key = ToPropertyKey(cx, propertyKey);
            /* step 3 */
            Property desc = targetObject.getOwnProperty(cx, key);
            /* step 4 */
            return FromPropertyDescriptor(cx, desc);
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
            /* step 1 */
            if (!Type.isObject(target)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject targetObject = Type.objectValue(target);
            /* step 2 */
            ScriptObject proto = targetObject.getPrototypeOf(cx);
            return proto != null ? proto : NULL;
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
        public static Object has(ExecutionContext cx, Object thisValue, Object target, Object propertyKey) {
            /* step 1 */
            if (!Type.isObject(target)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject targetObject = Type.objectValue(target);
            /* step 2 */
            Object key = ToPropertyKey(cx, propertyKey);
            /* step 3 */
            return targetObject.hasProperty(cx, key);
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
            /* step 1 */
            if (!Type.isObject(target)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject targetObject = Type.objectValue(target);
            /* step 2 */
            return targetObject.isExtensible(cx);
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
            /* step 1 */
            if (!Type.isObject(target)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject targetObject = Type.objectValue(target);
            /* step 2 */
            List<?> keys = targetObject.ownPropertyKeys(cx);
            /* step 3 */
            return CreateArrayFromList(cx, keys);
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
            /* step 1 */
            if (!Type.isObject(target)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject targetObject = Type.objectValue(target);
            /* step 2 */
            return targetObject.preventExtensions(cx);
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
        @Function(name = "set", arity = 3)
        public static Object set(ExecutionContext cx, Object thisValue, Object target, Object propertyKey, Object value,
                @Optional(Optional.Default.NONE) Object receiver) {
            /* step 1 */
            if (!Type.isObject(target)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject targetObject = Type.objectValue(target);
            /* step 2 */
            Object key = ToPropertyKey(cx, propertyKey);
            /* step 3 */
            if (receiver == null) {
                receiver = target;
            }
            /* step 4 */
            return targetObject.set(cx, key, value, receiver);
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
        public static Object setPrototypeOf(ExecutionContext cx, Object thisValue, Object target, Object proto) {
            /* step 1 */
            if (!Type.isObject(target)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject targetObject = Type.objectValue(target);
            /* step 2 */
            if (!Type.isObjectOrNull(proto)) {
                throw newTypeError(cx, Messages.Key.NotObjectOrNull);
            }
            /* step 3 */
            return targetObject.setPrototypeOf(cx, Type.objectValueOrNull(proto));
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
