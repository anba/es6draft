/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.internal.ListIterator.FromListIterator;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.FromPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.ToPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.1 Object Objects</h2>
 * <ul>
 * <li>19.1.1 The Object Constructor Called as a Function
 * <li>19.1.2 The Object Constructor
 * <li>19.1.3 Properties of the Object Constructor
 * </ul>
 */
public final class ObjectConstructor extends BuiltinConstructor implements Initialisable {
    public ObjectConstructor(Realm realm) {
        super(realm, "Object");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 19.1.1.1 Object ( [ value ] )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object value = args.length > 0 ? args[0] : UNDEFINED;
        /* step 1 */
        if (Type.isUndefinedOrNull(value)) {
            return ObjectCreate(calleeContext, Intrinsics.ObjectPrototype);
        }
        /* step 2 */
        return ToObject(calleeContext, value);
    }

    /**
     * 19.1.2.1 new Object ( [ value ] )
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return (ScriptObject) call(callerContext, UNDEFINED, args);
    }

    /**
     * 19.1.3 Properties of the Object Constructor
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
        public static final String name = "Object";

        /**
         * 19.1.3.16 Object.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.ObjectPrototype;

        /**
         * 19.1.3.9 Object.getPrototypeOf ( O )
         */
        @Function(name = "getPrototypeOf", arity = 1)
        public static Object getPrototypeOf(ExecutionContext cx, Object thisValue, Object o) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, o);
            /* step 3 */
            ScriptObject proto = obj.getPrototypeOf(cx);
            return proto != null ? proto : NULL;
        }

        /**
         * 19.1.3.6 Object.getOwnPropertyDescriptor ( O, P )
         */
        @Function(name = "getOwnPropertyDescriptor", arity = 2)
        public static Object getOwnPropertyDescriptor(ExecutionContext cx, Object thisValue,
                Object o, Object p) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, o);
            /* steps 3-4 */
            Object key = ToPropertyKey(cx, p);
            /* steps 5-6 */
            Property desc;
            if (key instanceof String) {
                desc = obj.getOwnProperty(cx, (String) key);
            } else {
                desc = obj.getOwnProperty(cx, (Symbol) key);
            }
            /* step 7 */
            return FromPropertyDescriptor(cx, desc);
        }

        /**
         * 19.1.3.7 Object.getOwnPropertyNames ( O )
         */
        @Function(name = "getOwnPropertyNames", arity = 1)
        public static Object getOwnPropertyNames(ExecutionContext cx, Object thisValue, Object o) {
            /* step 1 */
            return GetOwnPropertyNames(cx, o);
        }

        /**
         * 19.1.3.8 Object.getOwnPropertySymbols ( O )
         */
        @Function(name = "getOwnPropertySymbols", arity = 1)
        public static Object getOwnPropertySymbols(ExecutionContext cx, Object thisValue, Object o) {
            /* step 1 */
            return GetOwnPropertySymbols(cx, o);
        }

        /**
         * 19.1.3.2 Object.create ( O [, Properties] )
         */
        @Function(name = "create", arity = 2)
        public static Object create(ExecutionContext cx, Object thisValue, Object o,
                Object properties) {
            /* step 1 */
            if (!Type.isObjectOrNull(o)) {
                throw newTypeError(cx, Messages.Key.NotObjectOrNull);
            }
            ScriptObject proto = Type.objectValueOrNull(o);
            /* step 2 */
            ScriptObject obj = ObjectCreate(cx, proto);
            /* step 3 */
            if (!Type.isUndefined(properties)) {
                return ObjectDefineProperties(cx, obj, properties);
            }
            /* step 4 */
            return obj;
        }

        /**
         * 19.1.3.4 Object.defineProperty ( O, P, Attributes )
         */
        @Function(name = "defineProperty", arity = 3)
        public static Object defineProperty(ExecutionContext cx, Object thisValue, Object o,
                Object p, Object attributes) {
            /* step 1 */
            if (!Type.isObject(o)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* steps 2-3 */
            Object key = ToPropertyKey(cx, p);
            /* steps 4-5 */
            PropertyDescriptor desc = ToPropertyDescriptor(cx, attributes);
            /* steps 6-7 */
            DefinePropertyOrThrow(cx, Type.objectValue(o), key, desc);
            /* step 8 */
            return o;
        }

        /**
         * 19.1.3.3 Object.defineProperties ( O, Properties )
         */
        @Function(name = "defineProperties", arity = 2)
        public static Object defineProperties(ExecutionContext cx, Object thisValue, Object o,
                Object properties) {
            /* step 1 */
            return ObjectDefineProperties(cx, o, properties);
        }

        /**
         * 19.1.3.17 Object.seal ( O )
         */
        @Function(name = "seal", arity = 1)
        public static Object seal(ExecutionContext cx, Object thisValue, Object o) {
            /* step 1 */
            if (!Type.isObject(o)) {
                return o;
            }
            /* steps 2-3 */
            boolean status = SetIntegrityLevel(cx, Type.objectValue(o), IntegrityLevel.Sealed);
            /* step 4 */
            if (!status) {
                throw newTypeError(cx, Messages.Key.ObjectSealFailed);
            }
            /* step 5 */
            return o;
        }

        /**
         * 19.1.3.5 Object.freeze ( O )
         */
        @Function(name = "freeze", arity = 1)
        public static Object freeze(ExecutionContext cx, Object thisValue, Object o) {
            /* step 1 */
            if (!Type.isObject(o)) {
                return o;
            }
            /* steps 2-3 */
            boolean status = SetIntegrityLevel(cx, Type.objectValue(o), IntegrityLevel.Frozen);
            /* step 4 */
            if (!status) {
                throw newTypeError(cx, Messages.Key.ObjectFreezeFailed);
            }
            /* step 5 */
            return o;
        }

        /**
         * 19.1.3.15 Object.preventExtensions ( O )
         */
        @Function(name = "preventExtensions", arity = 1)
        public static Object preventExtensions(ExecutionContext cx, Object thisValue, Object o) {
            /* step 1 */
            if (!Type.isObject(o)) {
                return o;
            }
            /* steps 2-3 */
            boolean status = Type.objectValue(o).preventExtensions(cx);
            /* step 4 */
            if (!status) {
                throw newTypeError(cx, Messages.Key.ObjectPreventExtensionsFailed);
            }
            /* step 5 */
            return o;
        }

        /**
         * 19.1.3.13 Object.isSealed ( O )
         */
        @Function(name = "isSealed", arity = 1)
        public static Object isSealed(ExecutionContext cx, Object thisValue, Object o) {
            /* step 1 */
            if (!Type.isObject(o)) {
                return true;
            }
            /* step 2 */
            return TestIntegrityLevel(cx, Type.objectValue(o), IntegrityLevel.Sealed);
        }

        /**
         * 19.1.3.12 Object.isFrozen ( O )
         */
        @Function(name = "isFrozen", arity = 1)
        public static Object isFrozen(ExecutionContext cx, Object thisValue, Object o) {
            /* step 1 */
            if (!Type.isObject(o)) {
                return true;
            }
            /* step 2 */
            return TestIntegrityLevel(cx, Type.objectValue(o), IntegrityLevel.Frozen);
        }

        /**
         * 19.1.3.11 Object.isExtensible ( O )
         */
        @Function(name = "isExtensible", arity = 1)
        public static Object isExtensible(ExecutionContext cx, Object thisValue, Object o) {
            /* step 1 */
            if (!Type.isObject(o)) {
                return false;
            }
            /* step 2 */
            return IsExtensible(cx, Type.objectValue(o));
        }

        /**
         * 19.1.3.14 Object.keys ( O )
         */
        @Function(name = "keys", arity = 1)
        public static Object keys(ExecutionContext cx, Object thisValue, Object o) {
            /* steps 1-2 */
            ScriptObject obj = ToObject(cx, o);
            /* steps 3-7 */
            List<String> nameList = GetOwnEnumerablePropertyNames(cx, obj);
            /* step 8 */
            return CreateArrayFromList(cx, nameList);
        }

        /**
         * 19.1.3.10 Object.is ( value1, value2 )
         */
        @Function(name = "is", arity = 2)
        public static Object is(ExecutionContext cx, Object thisValue, Object value1, Object value2) {
            /* step 1 */
            return SameValue(value1, value2);
        }

        /**
         * 19.1.3.1 Object.assign ( target, source )
         */
        @Function(name = "assign", arity = 2)
        public static Object assign(ExecutionContext cx, Object thisValue, Object target,
                Object source) {
            /* steps 1-2 */
            ScriptObject to = ToObject(cx, target);
            /* steps 3-4 */
            ScriptObject from = ToObject(cx, source);
            /* steps 5-6 */
            Iterator<?> keys = FromListIterator(cx, from, from.ownPropertyKeys(cx));
            /* step 7 (omitted) */
            /* step 8 */
            ScriptException pendingException = null;
            /* step 9 */
            while (keys.hasNext()) {
                // FIXME: missing ToPropertyKey() call in specification
                Object nextKey = ToPropertyKey(cx, keys.next());
                try {
                    Property desc;
                    if (nextKey instanceof String) {
                        desc = from.getOwnProperty(cx, (String) nextKey);
                    } else {
                        desc = from.getOwnProperty(cx, (Symbol) nextKey);
                    }
                    if (desc != null && desc.isEnumerable()) {
                        Object propValue = Get(cx, from, nextKey);
                        Put(cx, to, nextKey, propValue, true);
                    }
                } catch (ScriptException e) {
                    if (pendingException == null) {
                        pendingException = e;
                    }
                }
            }
            /* step 10 */
            if (pendingException != null) {
                throw pendingException;
            }
            /* step 11 */
            return to;
        }

        /**
         * 19.1.3.18 Object.setPrototypeOf ( O, proto )
         */
        @Function(name = "setPrototypeOf", arity = 2)
        public static Object setPrototypeOf(ExecutionContext cx, Object thisValue, Object o,
                Object proto) {
            /* steps 1-2 */
            CheckObjectCoercible(cx, o);
            /* step 3 */
            if (!Type.isObjectOrNull(proto)) {
                throw newTypeError(cx, Messages.Key.NotObjectOrNull);
            }
            /* step 4 */
            if (!Type.isObject(o)) {
                return o;
            }
            /* steps 5-6 */
            ScriptObject obj = Type.objectValue(o);
            ScriptObject p = Type.objectValueOrNull(proto);
            boolean status = obj.setPrototypeOf(cx, p);
            /* step 7 */
            if (!status) {
                // provide better error messages for ordinary objects
                if (obj instanceof OrdinaryObject) {
                    if (!obj.isExtensible(cx)) {
                        throw newTypeError(cx, Messages.Key.NotExtensible);
                    }
                    throw newTypeError(cx, Messages.Key.CyclicProto);
                }
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 8 */
            return obj;
        }
    }

    /**
     * 19.1.3.3 Object.defineProperties ( O, Properties )
     * <p>
     * Runtime Semantics: ObjectDefineProperties Abstract Operation
     */
    public static ScriptObject ObjectDefineProperties(ExecutionContext cx, Object o,
            Object properties) {
        /* step 1 */
        if (!Type.isObject(o)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        ScriptObject obj = Type.objectValue(o);
        /* step 2 */
        ScriptObject props = ToObject(cx, properties);
        /* step 3 */
        List<Object> names = GetOwnEnumerablePropertyKeys(cx, props);
        /* step 4 */
        List<PropertyDescriptor> descriptors = new ArrayList<>();
        /* step 5 */
        for (Object p : names) {
            Object descObj = Get(cx, props, p);
            PropertyDescriptor desc = ToPropertyDescriptor(cx, descObj);
            descriptors.add(desc);
        }
        /* step 6 */
        ScriptException pendingException = null;
        /* step 7 */
        for (int i = 0, size = names.size(); i < size; ++i) {
            Object p = names.get(i);
            PropertyDescriptor desc = descriptors.get(i);
            try {
                DefinePropertyOrThrow(cx, obj, p, desc);
            } catch (ScriptException e) {
                if (pendingException == null) {
                    pendingException = e;
                }
            }
        }
        /* step 8 */
        if (pendingException != null) {
            throw pendingException;
        }
        /* step 9 */
        return obj;
    }

    /**
     * 19.1.3.8.1 GetOwnPropertyKey ( O, Type ) Abstract Operation, with Type = String
     */
    public static ScriptObject GetOwnPropertyNames(ExecutionContext cx, Object o) {
        /* steps 1-2 */
        ScriptObject obj = ToObject(cx, o);
        /* steps 3-4 */
        Iterator<?> keys = FromListIterator(cx, obj, obj.ownPropertyKeys(cx));
        /* step 5 */
        List<String> nameList = new ArrayList<>();
        /* step 6 (omitted) */
        /* step 7 */
        while (keys.hasNext()) {
            Object key = ToPropertyKey(cx, keys.next());
            if (key instanceof String) {
                nameList.add((String) key);
            }
        }
        /* step 8 */
        return CreateArrayFromList(cx, nameList);
    }

    /**
     * 19.1.3.8.1 GetOwnPropertyKey ( O, Type ) Abstract Operation, with Type = Symbol
     */
    public static ScriptObject GetOwnPropertySymbols(ExecutionContext cx, Object o) {
        /* steps 1-2 */
        ScriptObject obj = ToObject(cx, o);
        /* steps 3-4 */
        Iterator<?> keys = FromListIterator(cx, obj, obj.ownPropertyKeys(cx));
        /* step 5 */
        List<Symbol> nameList = new ArrayList<>();
        /* step 6 (omitted) */
        /* step 7 */
        while (keys.hasNext()) {
            Object key = ToPropertyKey(cx, keys.next());
            if (key instanceof Symbol) {
                nameList.add((Symbol) key);
            }
        }
        /* step 8 */
        return CreateArrayFromList(cx, nameList);
    }
}
