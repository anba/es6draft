/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.HasOwnProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToPropertyKey;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.FromPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.ToPropertyDescriptor;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
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
public final class Reflect extends OrdinaryObject implements Initialisable {
    public Reflect(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        createProperties(this, cx, AdditionalProperties.class);
    }

    /**
     * 26.1 Properties of the Reflect Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 26.1.6 Reflect.getPrototypeOf (target)
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
         * 26.1.13 Reflect.setPrototypeOf (target, proto)
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
            ScriptObject p = Type.objectValueOrNull(proto);
            return obj.setPrototypeOf(cx, p);
        }

        /**
         * 26.1.9 Reflect.isExtensible (target)
         */
        @Function(name = "isExtensible", arity = 1)
        public static Object isExtensible(ExecutionContext cx, Object thisValue, Object target) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* step 3 */
            return obj.isExtensible(cx);
        }

        /**
         * 26.1.11 Reflect.preventExtensions (target)
         */
        @Function(name = "preventExtensions", arity = 1)
        public static Object preventExtensions(ExecutionContext cx, Object thisValue, Object target) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* step 3 */
            return obj.preventExtensions(cx);
        }

        /**
         * 26.1.7 Reflect.has (target, propertyKey)
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
         * 26.1.8 Reflect.hasOwn (target, propertyKey)
         */
        @Function(name = "hasOwn", arity = 2)
        public static Object hasOwn(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey) {
            // TODO function still relevant after [[HasOwn]] removal from MOP?
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* steps 3-4 */
            Object key = ToPropertyKey(cx, propertyKey);
            /* step 5 */
            if (key instanceof String) {
                return HasOwnProperty(cx, obj, (String) key);
            } else {
                assert key instanceof Symbol;
                return HasOwnProperty(cx, obj, (Symbol) key);
            }
        }

        /**
         * 26.1.5 Reflect.getOwnPropertyDescriptor(target, propertyKey)
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
         * 26.1.4 Reflect.get (target, propertyKey, receiver=target)
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
         * 26.1.12 Reflect.set (target, propertyKey, V, receiver=target)
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
         * 26.1.2 Reflect.deleteProperty (target, propertyKey)
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
         * 26.1.1 Reflect.defineProperty(target, propertyKey, attributes)
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
         * 26.1.3 Reflect.enumerate (target)
         */
        @Function(name = "enumerate", arity = 1)
        public static Object enumerate(ExecutionContext cx, Object thisValue, Object target) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* steps 3-4 */
            return obj.enumerate(cx);
        }

        /**
         * 26.1.10 Reflect.ownKeys (target)
         */
        @Function(name = "ownKeys", arity = 1)
        public static Object ownKeys(ExecutionContext cx, Object thisValue, Object target) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, target);
            /* step 3 */
            return obj.ownPropertyKeys(cx);
        }
    }

    @CompatibilityExtension(CompatibilityOption.ReflectParse)
    public enum AdditionalProperties {
        ;

        /**
         * Reflect.parse(src[, options])
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
}
