/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsExtensible;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToPropertyKey;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.ToPropertyDescriptor;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 15.17 The Reflect Module
 * <p>
 * 15.17.1 Exported Function Properties Reflecting the Essentional Internal Methods<br>
 * 
 * 
 * TODO: remove representation as ordinary object
 */
public class Reflect extends OrdinaryObject implements ScriptObject, Initialisable, Module {
    public Reflect(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        setPrototype(realm, null);
        setIntegrity(realm, IntegrityLevel.NonExtensible);

        createProperties(this, realm, ReflectedFunctions.class);
    }

    /**
     * 15.17.1 Exported Function Properties Reflecting the Essentional Internal Methods
     */
    public enum ReflectedFunctions {
        ;

        /**
         * 15.17.1.1 Reflect.getPrototypeOf (target)
         */
        @Function(name = "getPrototypeOf", arity = 1)
        public static Object getPrototypeOf(Realm realm, Object thisValue, Object target) {
            ScriptObject obj = ToObject(realm, target);
            return obj.getPrototype(realm);
        }

        /**
         * 15.17.1.2 Reflect.setPrototypeOf (target, proto)
         */
        @Function(name = "setPrototypeOf", arity = 2)
        public static Object setPrototypeOf(Realm realm, Object thisValue, Object target,
                Object proto) {
            ScriptObject obj = ToObject(realm, target);
            if (!(Type.isObject(proto) || Type.isNull(proto))) {
                throw throwTypeError(realm, Messages.Key.NotObjectOrNull);
            }
            ScriptObject p = Type.isObject(proto) ? Type.objectValue(proto) : null;
            return obj.setPrototype(realm, p);
        }

        /**
         * 15.17.1.3 Reflect.isExtensible (target)
         */
        @Function(name = "isExtensible", arity = 1)
        public static Object isExtensible(Realm realm, Object thisValue, Object target) {
            ScriptObject obj = ToObject(realm, target);
            return IsExtensible(realm, obj);
        }

        /**
         * 15.17.1.4 Reflect.preventExtensions (target)
         */
        @Function(name = "preventExtensions", arity = 1)
        public static Object preventExtensions(Realm realm, Object thisValue, Object target) {
            ScriptObject obj = ToObject(realm, target);
            return obj.setIntegrity(realm, IntegrityLevel.NonExtensible);
        }

        /**
         * 15.17.1.5 Reflect.has (target, propertyKey)
         */
        @Function(name = "has", arity = 2)
        public static Object has(Realm realm, Object thisValue, Object target, Object propertyKey) {
            ScriptObject obj = ToObject(realm, target);
            Object key = ToPropertyKey(realm, propertyKey);
            if (key instanceof String) {
                return obj.hasProperty(realm, (String) key);
            } else {
                assert key instanceof Symbol;
                return obj.hasProperty(realm, (Symbol) key);
            }
        }

        /**
         * 15.17.1.6 Reflect.hasOwn (target, propertyKey)
         */
        @Function(name = "hasOwn", arity = 2)
        public static Object hasOwn(Realm realm, Object thisValue, Object target, Object propertyKey) {
            ScriptObject obj = ToObject(realm, target);
            Object key = ToPropertyKey(realm, propertyKey);
            if (key instanceof String) {
                return obj.hasOwnProperty(realm, (String) key);
            } else {
                assert key instanceof Symbol;
                return obj.hasOwnProperty(realm, (Symbol) key);
            }
        }

        /**
         * 15.17.1.7 Reflect.getOwnPropertyDescriptor(target, propertyKey)
         */
        @Function(name = "getOwnPropertyDescriptor", arity = 2)
        public static Object getOwnPropertyDescriptor(Realm realm, Object thisValue, Object target,
                Object propertyKey) {
            ScriptObject obj = ToObject(realm, target);
            Object key = ToPropertyKey(realm, propertyKey);
            // FIXME: spec bug [[GetOwnProperty]] return value returned as-is!
            if (key instanceof String) {
                return obj.getOwnProperty(realm, (String) key);
            } else {
                assert key instanceof Symbol;
                return obj.getOwnProperty(realm, (Symbol) key);
            }
        }

        /**
         * 15.17.1.8 Reflect.get (target, propertyKey, receiver=target)
         */
        @Function(name = "get", arity = 3)
        public static Object get(Realm realm, Object thisValue, Object target, Object propertyKey,
                @Optional(Optional.Default.NONE) Object receiver) {
            if (receiver == null) {
                receiver = target;
            }
            ScriptObject obj = ToObject(realm, target);
            Object key = ToPropertyKey(realm, propertyKey);
            if (key instanceof String) {
                return obj.get(realm, (String) key, receiver);
            } else {
                assert key instanceof Symbol;
                return obj.get(realm, (Symbol) key, receiver);
            }
        }

        /**
         * 15.17.1.9 Reflect.set (target, propertyKey, V, receiver=target)
         */
        @Function(name = "set", arity = 4)
        public static Object set(Realm realm, Object thisValue, Object target, Object propertyKey,
                Object value, @Optional(Optional.Default.NONE) Object receiver) {
            if (receiver == null) {
                receiver = target;
            }
            ScriptObject obj = ToObject(realm, target);
            Object key = ToPropertyKey(realm, propertyKey);
            if (key instanceof String) {
                return obj.set(realm, (String) key, value, receiver);
            } else {
                assert key instanceof Symbol;
                return obj.set(realm, (Symbol) key, value, receiver);
            }
        }

        /**
         * 15.17.1.10 Reflect.deleteProperty (target, propertyKey)
         */
        @Function(name = "deleteProperty", arity = 2)
        public static Object deleteProperty(Realm realm, Object thisValue, Object target,
                Object propertyKey) {
            ScriptObject obj = ToObject(realm, target);
            Object key = ToPropertyKey(realm, propertyKey);
            if (key instanceof String) {
                return obj.delete(realm, (String) key);
            } else {
                assert key instanceof Symbol;
                return obj.delete(realm, (Symbol) key);
            }
        }

        /**
         * 15.17.1.11 Reflect.defineProperty(target, propertyKey, Attributes)
         */
        @Function(name = "defineProperty", arity = 3)
        public static Object defineProperty(Realm realm, Object thisValue, Object target,
                Object propertyKey, Object attributes) {
            ScriptObject obj = ToObject(realm, target);
            Object key = ToPropertyKey(realm, propertyKey);
            PropertyDescriptor desc = ToPropertyDescriptor(realm, attributes);
            if (key instanceof String) {
                return obj.defineOwnProperty(realm, (String) key, desc);
            } else {
                assert key instanceof Symbol;
                return obj.defineOwnProperty(realm, (Symbol) key, desc);
            }
        }

        /**
         * 15.17.1.12 Reflect.enumerate (target)
         */
        @Function(name = "enumerate", arity = 1)
        public static Object enumerate(Realm realm, Object thisValue, Object target) {
            ScriptObject obj = ToObject(realm, target);
            ScriptObject itr = obj.enumerate(realm);
            return itr;
        }

        /**
         * 15.17.1.13 Reflect.ownKeys (target)
         */
        @Function(name = "ownKeys", arity = 1)
        public static Object ownKeys(Realm realm, Object thisValue, Object target) {
            ScriptObject obj = ToObject(realm, target);
            ScriptObject keys = obj.ownPropertyKeys(realm);
            // FIXME: spec bug (algorithm end at step 4 without return)
            return keys;
        }

        /**
         * 15.17.1.14 Reflect.freeze (target)
         */
        @Function(name = "freeze", arity = 1)
        public static Object freeze(Realm realm, Object thisValue, Object target) {
            ScriptObject obj = ToObject(realm, target);
            return obj.setIntegrity(realm, IntegrityLevel.Frozen);
        }

        /**
         * 15.17.1.15 Reflect.seal (target)
         */
        @Function(name = "seal", arity = 1)
        public static Object seal(Realm realm, Object thisValue, Object target) {
            ScriptObject obj = ToObject(realm, target);
            return obj.setIntegrity(realm, IntegrityLevel.Sealed);
        }

        /**
         * 15.17.1.16 Reflect.isFrozen (target)
         */
        @Function(name = "isFrozen", arity = 1)
        public static Object isFrozen(Realm realm, Object thisValue, Object target) {
            ScriptObject obj = ToObject(realm, target);
            return obj.hasIntegrity(realm, IntegrityLevel.Frozen);
        }

        /**
         * 15.17.1.17 Reflect.isSealed (target)
         */
        @Function(name = "isSealed", arity = 1)
        public static Object isSealed(Realm realm, Object thisValue, Object target) {
            ScriptObject obj = ToObject(realm, target);
            return obj.hasIntegrity(realm, IntegrityLevel.Sealed);
        }
    }
}
