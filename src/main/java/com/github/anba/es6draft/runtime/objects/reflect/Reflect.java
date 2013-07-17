/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateListFromArrayLike;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToPropertyKey;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.FromPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.ToPropertyDescriptor;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.modules.Module;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ExoticSymbol;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 15.18 The Reflect Module
 * <p>
 * 15.18.1 Exported Function Properties Reflecting the Essentional Internal Methods<br>
 * 
 * TODO: remove representation as ordinary object
 */
public class Reflect extends OrdinaryObject implements Initialisable, Module {
    public Reflect(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, ReflectedFunctions.class);
        preventExtensions(cx);
    }

    /**
     * 15.18.1 Exported Function Properties Reflecting the Essentional Internal Methods
     */
    public enum ReflectedFunctions {
        ;

        @Prototype
        public static final Intrinsics __proto__ = null;

        /**
         * 15.18.1.1 Reflect.getPrototypeOf (target)
         */
        @Function(name = "getPrototypeOf", arity = 1)
        public static Object getPrototypeOf(ExecutionContext cx, Object thisValue, Object target) {
            ScriptObject obj = ToObject(cx, target);
            ScriptObject proto = obj.getInheritance(cx);
            return (proto != null ? proto : NULL);
        }

        /**
         * 15.18.1.2 Reflect.setPrototypeOf (target, proto)
         */
        @Function(name = "setPrototypeOf", arity = 2)
        public static Object setPrototypeOf(ExecutionContext cx, Object thisValue, Object target,
                Object proto) {
            ScriptObject obj = ToObject(cx, target);
            if (!(Type.isObject(proto) || Type.isNull(proto))) {
                throw throwTypeError(cx, Messages.Key.NotObjectOrNull);
            }
            ScriptObject p = Type.isObject(proto) ? Type.objectValue(proto) : null;
            return obj.setInheritance(cx, p);
        }

        /**
         * 15.18.1.3 Reflect.isExtensible (target)
         */
        @Function(name = "isExtensible", arity = 1)
        public static Object isExtensible(ExecutionContext cx, Object thisValue, Object target) {
            ScriptObject obj = ToObject(cx, target);
            return obj.isExtensible(cx);
        }

        /**
         * 15.18.1.4 Reflect.preventExtensions (target)
         */
        @Function(name = "preventExtensions", arity = 1)
        public static Object preventExtensions(ExecutionContext cx, Object thisValue, Object target) {
            ScriptObject obj = ToObject(cx, target);
            return obj.preventExtensions(cx);
        }

        /**
         * 15.18.1.5 Reflect.has (target, propertyKey)
         */
        @Function(name = "has", arity = 2)
        public static Object has(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey) {
            ScriptObject obj = ToObject(cx, target);
            Object key = ToPropertyKey(cx, propertyKey);
            if (key instanceof String) {
                return obj.hasProperty(cx, (String) key);
            } else {
                assert key instanceof ExoticSymbol;
                return obj.hasProperty(cx, (ExoticSymbol) key);
            }
        }

        /**
         * 15.18.1.6 Reflect.hasOwn (target, propertyKey)
         */
        @Function(name = "hasOwn", arity = 2)
        public static Object hasOwn(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey) {
            ScriptObject obj = ToObject(cx, target);
            Object key = ToPropertyKey(cx, propertyKey);
            if (key instanceof String) {
                return obj.hasOwnProperty(cx, (String) key);
            } else {
                assert key instanceof ExoticSymbol;
                return obj.hasOwnProperty(cx, (ExoticSymbol) key);
            }
        }

        /**
         * 15.18.1.7 Reflect.getOwnPropertyDescriptor(target, propertyKey)
         */
        @Function(name = "getOwnPropertyDescriptor", arity = 2)
        public static Object getOwnPropertyDescriptor(ExecutionContext cx, Object thisValue,
                Object target, Object propertyKey) {
            ScriptObject obj = ToObject(cx, target);
            Object key = ToPropertyKey(cx, propertyKey);
            Property desc;
            if (key instanceof String) {
                desc = obj.getOwnProperty(cx, (String) key);
            } else {
                assert key instanceof ExoticSymbol;
                desc = obj.getOwnProperty(cx, (ExoticSymbol) key);
            }
            return FromPropertyDescriptor(cx, desc);
        }

        /**
         * 15.18.1.8 Reflect.get (target, propertyKey, receiver=target)
         */
        @Function(name = "get", arity = 3)
        public static Object get(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey, @Optional(Optional.Default.NONE) Object receiver) {
            if (receiver == null) {
                receiver = target;
            }
            ScriptObject obj = ToObject(cx, target);
            Object key = ToPropertyKey(cx, propertyKey);
            if (key instanceof String) {
                return obj.get(cx, (String) key, receiver);
            } else {
                assert key instanceof ExoticSymbol;
                return obj.get(cx, (ExoticSymbol) key, receiver);
            }
        }

        /**
         * 15.18.1.9 Reflect.set (target, propertyKey, V, receiver=target)
         */
        @Function(name = "set", arity = 4)
        public static Object set(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey, Object value, @Optional(Optional.Default.NONE) Object receiver) {
            if (receiver == null) {
                receiver = target;
            }
            ScriptObject obj = ToObject(cx, target);
            Object key = ToPropertyKey(cx, propertyKey);
            if (key instanceof String) {
                return obj.set(cx, (String) key, value, receiver);
            } else {
                assert key instanceof ExoticSymbol;
                return obj.set(cx, (ExoticSymbol) key, value, receiver);
            }
        }

        /**
         * 15.18.1.10 Reflect.invoke (target, propertyKey, argumentsList, receiver=target)
         */
        @Function(name = "invoke", arity = 4)
        public static Object deleteProperty(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey, Object argumentsList,
                @Optional(Optional.Default.NONE) Object receiver) {
            ScriptObject obj = ToObject(cx, target);
            Object key = ToPropertyKey(cx, propertyKey);
            if (receiver == null) {
                receiver = target;
            }
            Object[] argList = CreateListFromArrayLike(cx, argumentsList);
            if (key instanceof String) {
                return obj.invoke(cx, (String) key, argList, receiver);
            } else {
                assert key instanceof ExoticSymbol;
                return obj.invoke(cx, (ExoticSymbol) key, argList, receiver);
            }
        }

        /**
         * 15.18.1.10 Reflect.deleteProperty (target, propertyKey)
         */
        @Function(name = "deleteProperty", arity = 2)
        public static Object deleteProperty(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey) {
            ScriptObject obj = ToObject(cx, target);
            Object key = ToPropertyKey(cx, propertyKey);
            if (key instanceof String) {
                return obj.delete(cx, (String) key);
            } else {
                assert key instanceof ExoticSymbol;
                return obj.delete(cx, (ExoticSymbol) key);
            }
        }

        /**
         * 15.18.1.11 Reflect.defineProperty(target, propertyKey, Attributes)
         */
        @Function(name = "defineProperty", arity = 3)
        public static Object defineProperty(ExecutionContext cx, Object thisValue, Object target,
                Object propertyKey, Object attributes) {
            ScriptObject obj = ToObject(cx, target);
            Object key = ToPropertyKey(cx, propertyKey);
            PropertyDescriptor desc = ToPropertyDescriptor(cx, attributes);
            if (key instanceof String) {
                return obj.defineOwnProperty(cx, (String) key, desc);
            } else {
                assert key instanceof ExoticSymbol;
                return obj.defineOwnProperty(cx, (ExoticSymbol) key, desc);
            }
        }

        /**
         * 15.18.1.12 Reflect.enumerate (target)
         */
        @Function(name = "enumerate", arity = 1)
        public static Object enumerate(ExecutionContext cx, Object thisValue, Object target) {
            ScriptObject obj = ToObject(cx, target);
            ScriptObject itr = obj.enumerate(cx);
            return itr;
        }

        /**
         * 15.18.1.13 Reflect.ownKeys (target)
         */
        @Function(name = "ownKeys", arity = 1)
        public static Object ownKeys(ExecutionContext cx, Object thisValue, Object target) {
            ScriptObject obj = ToObject(cx, target);
            ScriptObject keys = obj.ownPropertyKeys(cx);
            // FIXME: spec bug (algorithm end at step 4 without return)
            return keys;
        }
    }
}
