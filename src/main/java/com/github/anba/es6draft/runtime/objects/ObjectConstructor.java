/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.internal.ListIterator.FromListIterator;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.FromPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.ToPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.GetSuperBinding;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.RebindSuper;

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
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.ExoticSymbol;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
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
public class ObjectConstructor extends BuiltinConstructor implements Initialisable {
    public ObjectConstructor(Realm realm) {
        super(realm);
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
        if (Type.isUndefinedOrNull(value)) {
            return ObjectCreate(calleeContext, Intrinsics.ObjectPrototype);
        }
        return ToObject(calleeContext, value);
    }

    /**
     * 19.1.2.1 new Object ( [ value ] )
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        // FIXME: spec issue? (should possibly call %Object%[[Call]], execution-context/realm!)
        ExecutionContext calleeContext = calleeContext();
        if (args.length > 0) {
            Object value = args[0];
            switch (Type.of(value)) {
            case Object:
                return Type.objectValue(value);
            case String:
            case Boolean:
            case Number:
                return ToObject(calleeContext, value);
            case Null:
            case Undefined:
            default:
                break;
            }
        }
        return ObjectCreate(calleeContext, Intrinsics.ObjectPrototype);
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
                configurable = false))
        public static final String name = "Object";

        /**
         * 19.1.3.17 Object.prototype
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
            ScriptObject proto = obj.getInheritance(cx);
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
                desc = obj.getOwnProperty(cx, (ExoticSymbol) key);
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
            if (!(Type.isObject(o) || Type.isNull(o))) {
                throw throwTypeError(cx, Messages.Key.NotObjectOrNull);
            }
            ScriptObject proto = Type.isObject(o) ? Type.objectValue(o) : null;
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
                throw throwTypeError(cx, Messages.Key.NotObjectType);
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
         * 19.1.3.18 Object.seal ( O )
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
                throw throwTypeError(cx, Messages.Key.ObjectSealFailed);
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
                throw throwTypeError(cx, Messages.Key.ObjectFreezeFailed);
            }
            /* step 5 */
            return o;
        }

        /**
         * 19.1.3.16 Object.preventExtensions ( O )
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
                throw throwTypeError(cx, Messages.Key.ObjectPreventExtensionsFailed);
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
            Iterator<?> keys = FromListIterator(cx, from.ownPropertyKeys(cx));
            /* step 7 (omitted) */
            /* step 8 */
            ScriptException pendingException = null;
            /* step 9 */
            while (keys.hasNext()) {
                Object nextKey = ToPropertyKey(cx, keys.next());
                try {
                    Property desc;
                    if (nextKey instanceof String) {
                        desc = from.getOwnProperty(cx, (String) nextKey);
                    } else {
                        desc = from.getOwnProperty(cx, (ExoticSymbol) nextKey);
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
         * 19.1.3.15 Object.mixin ( target, source )
         */
        @Function(name = "mixin", arity = 2)
        public static Object mixin(ExecutionContext cx, Object thisValue, Object target,
                Object source) {
            /* steps 1-2 */
            ScriptObject to = Type.objectValue(target);
            /* steps 3-4 */
            ScriptObject from = Type.objectValue(source);
            /* step 5 */
            return MixinProperties(cx, to, from);
        }

        /**
         * 19.1.3.19 Object.setPrototypeOf ( O, proto )
         */
        @Function(name = "setPrototypeOf", arity = 2)
        public static Object setPrototypeOf(ExecutionContext cx, Object thisValue, Object o,
                Object proto) {
            /* steps 1-2 */
            CheckObjectCoercible(cx, o);
            /* step 3 (empty) */
            /* step 4 */
            if (!(Type.isNull(proto) || Type.isObject(proto))) {
                throw throwTypeError(cx, Messages.Key.NotObjectOrNull);
            }
            /* step 5 */
            if (!Type.isObject(o)) {
                return o;
            }
            /* steps 6-7 */
            ScriptObject obj = Type.objectValue(o);
            ScriptObject p = Type.isObject(proto) ? Type.objectValue(proto) : null;
            boolean status = obj.setInheritance(cx, p);
            /* step 8 */
            if (!status) {
                // provide better error messages for ordinary objects
                if (obj instanceof OrdinaryObject) {
                    if (!obj.isExtensible(cx)) {
                        throw throwTypeError(cx, Messages.Key.NotExtensible);
                    }
                    throw throwTypeError(cx, Messages.Key.CyclicProto);
                }
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 9 */
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
            throw throwTypeError(cx, Messages.Key.NotObjectType);
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
        Iterator<?> keys = FromListIterator(cx, obj.ownPropertyKeys(cx));
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
        Iterator<?> keys = FromListIterator(cx, obj.ownPropertyKeys(cx));
        /* step 5 */
        List<ExoticSymbol> nameList = new ArrayList<>();
        /* step 6 (omitted) */
        /* step 7 */
        while (keys.hasNext()) {
            Object key = ToPropertyKey(cx, keys.next());
            if (key instanceof ExoticSymbol) {
                nameList.add((ExoticSymbol) key);
            }
        }
        /* step 8 */
        return CreateArrayFromList(cx, nameList);
    }

    /**
     * 19.1.3.15.1 MixinProperties( target, source )
     */
    public static <OBJECT extends ScriptObject> OBJECT MixinProperties(ExecutionContext cx,
            OBJECT target, ScriptObject source) {
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        Iterator<?> keys = FromListIterator(cx, source.ownPropertyKeys(cx));
        /* step 5 (omitted) */
        /* step 6 */
        ScriptException pendingException = null;
        /* step 7 */
        while (keys.hasNext()) {
            Object nextKey = ToPropertyKey(cx, keys.next());
            Property prop = null;
            try {
                if (nextKey instanceof String) {
                    prop = source.getOwnProperty(cx, (String) nextKey);
                } else {
                    prop = source.getOwnProperty(cx, (ExoticSymbol) nextKey);
                }
            } catch (ScriptException e) {
                if (pendingException == null) {
                    pendingException = e;
                }
            }
            if (prop == null || !prop.isEnumerable()) {
                continue;
            }
            PropertyDescriptor desc = prop.toPropertyDescriptor();
            if (desc.isDataDescriptor()) {
                Object propValue = desc.getValue();
                if (SameValue(GetSuperBinding(propValue), source)) {
                    try {
                        FunctionObject f = (FunctionObject) propValue;
                        FunctionObject newFunc = MixinProperties(cx, RebindSuper(cx, f, target), f);
                        desc.setValue(newFunc);
                    } catch (ScriptException e) {
                        if (pendingException == null) {
                            pendingException = e;
                        }
                    }
                }
            } else {
                assert desc.isAccessorDescriptor();
                Callable getter = desc.getGetter();
                if (SameValue(GetSuperBinding(getter), source)) {
                    try {
                        FunctionObject f = (FunctionObject) getter;
                        FunctionObject newFunc = MixinProperties(cx, RebindSuper(cx, f, target), f);
                        desc.setGetter(newFunc);
                    } catch (ScriptException e) {
                        if (pendingException == null) {
                            pendingException = e;
                        }
                    }
                }
                Callable setter = desc.getSetter();
                if (SameValue(GetSuperBinding(setter), source)) {
                    try {
                        FunctionObject f = (FunctionObject) setter;
                        FunctionObject newFunc = MixinProperties(cx, RebindSuper(cx, f, target), f);
                        desc.setSetter(newFunc);
                    } catch (ScriptException e) {
                        if (pendingException == null) {
                            pendingException = e;
                        }
                    }
                }
            }
            try {
                DefinePropertyOrThrow(cx, target, nextKey, desc);
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
        return target;
    }
}
