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
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.FromPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.ToPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ListIterator.FromListIterator;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator.GeneratorFunctionCreate;

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
import com.github.anba.es6draft.runtime.types.*;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.2 Object Objects</h2>
 * <ul>
 * <li>15.2.1 The Object Constructor Called as a Function
 * <li>15.2.2 The Object Constructor
 * <li>15.2.3 Properties of the Object Constructor
 * </ul>
 */
public class ObjectConstructor extends BuiltinFunction implements Constructor, Initialisable {
    public ObjectConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 15.2.1.1 Object ( [ value ] )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = realm().defaultContext();
        Object value = args.length > 0 ? args[0] : UNDEFINED;
        if (Type.isUndefinedOrNull(value)) {
            return ObjectCreate(calleeContext, Intrinsics.ObjectPrototype);
        }
        return ToObject(calleeContext, value);
    }

    /**
     * 15.2.2.1 new Object ( [ value ] )
     */
    @Override
    public Object construct(ExecutionContext callerContext, Object... args) {
        // FIXME: spec issue? (should possibly call %Object%[[Call]], execution-context/realm!)
        ExecutionContext calleeContext = realm().defaultContext();
        if (args.length > 0) {
            Object value = args[0];
            switch (Type.of(value)) {
            case Object:
                return value;
            case String:
            case Boolean:
            case Number:
            case Symbol:
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
     * 15.2.3 Properties of the Object Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "Object";

        /**
         * 15.2.3.1 Object.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.ObjectPrototype;

        /**
         * 15.2.3.2 Object.getPrototypeOf ( O )
         */
        @Function(name = "getPrototypeOf", arity = 1)
        public static Object getPrototypeOf(ExecutionContext cx, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject proto = Type.objectValue(o).getPrototype(cx);
            if (proto != null) {
                return proto;
            }
            return NULL;
        }

        /**
         * 15.2.3.3 Object.getOwnPropertyDescriptor ( O, P )
         */
        @Function(name = "getOwnPropertyDescriptor", arity = 2)
        public static Object getOwnPropertyDescriptor(ExecutionContext cx, Object thisValue,
                Object o, Object p) {
            if (!Type.isObject(o)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            Object key = ToPropertyKey(cx, p);
            Property desc;
            if (key instanceof String) {
                desc = Type.objectValue(o).getOwnProperty(cx, (String) key);
            } else {
                desc = Type.objectValue(o).getOwnProperty(cx, (Symbol) key);
            }
            return FromPropertyDescriptor(cx, desc);
        }

        /**
         * 15.2.3.4 Object.getOwnPropertyNames ( O )
         */
        @Function(name = "getOwnPropertyNames", arity = 1)
        public static Object getOwnPropertyNames(ExecutionContext cx, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            List<String> nameList = GetOwnPropertyNames(cx, Type.objectValue(o));
            return CreateArrayFromList(cx, nameList);
        }

        /**
         * 15.2.3.5 Object.create ( O [, Properties] )
         */
        @Function(name = "create", arity = 2)
        public static Object create(ExecutionContext cx, Object thisValue, Object o,
                Object properties) {
            if (!(Type.isObject(o) || Type.isNull(o))) {
                throw throwTypeError(cx, Messages.Key.NotObjectOrNull);
            }
            ScriptObject proto = Type.isObject(o) ? Type.objectValue(o) : null;
            ScriptObject obj = ObjectCreate(cx, proto);
            if (!Type.isUndefined(properties)) {
                return ObjectDefineProperties(cx, obj, properties);
            }
            return obj;
        }

        /**
         * 15.2.3.6 Object.defineProperty ( O, P, Attributes )
         */
        @Function(name = "defineProperty", arity = 3)
        public static Object defineProperty(ExecutionContext cx, Object thisValue, Object o,
                Object p, Object attributes) {
            if (!Type.isObject(o)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            Object key = ToPropertyKey(cx, p);
            PropertyDescriptor desc = ToPropertyDescriptor(cx, attributes);
            if (key instanceof String) {
                DefinePropertyOrThrow(cx, Type.objectValue(o), (String) key, desc);
            } else {
                DefinePropertyOrThrow(cx, Type.objectValue(o), (Symbol) key, desc);
            }
            return o;
        }

        /**
         * 15.2.3.7 Object.defineProperties ( O, Properties )
         */
        @Function(name = "defineProperties", arity = 2)
        public static Object defineProperties(ExecutionContext cx, Object thisValue, Object o,
                Object properties) {
            return ObjectDefineProperties(cx, o, properties);
        }

        /**
         * 15.2.3.8 Object.seal ( O )
         */
        @Function(name = "seal", arity = 1)
        public static Object seal(ExecutionContext cx, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            boolean status = Type.objectValue(o).setIntegrity(cx, IntegrityLevel.Sealed);
            if (!status) {
                throw throwTypeError(cx, Messages.Key.ObjectSealFailed);
            }
            return o;
        }

        /**
         * 15.2.3.9 Object.freeze ( O )
         */
        @Function(name = "freeze", arity = 1)
        public static Object freeze(ExecutionContext cx, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            boolean status = Type.objectValue(o).setIntegrity(cx, IntegrityLevel.Frozen);
            if (!status) {
                throw throwTypeError(cx, Messages.Key.ObjectFreezeFailed);
            }
            return o;
        }

        /**
         * 15.2.3.10 Object.preventExtensions ( O )
         */
        @Function(name = "preventExtensions", arity = 1)
        public static Object preventExtensions(ExecutionContext cx, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            boolean status = Type.objectValue(o).setIntegrity(cx, IntegrityLevel.NonExtensible);
            if (!status) {
                throw throwTypeError(cx, Messages.Key.ObjectPreventExtensionsFailed);
            }
            return o;
        }

        /**
         * 15.2.3.11 Object.isSealed ( O )
         */
        @Function(name = "isSealed", arity = 1)
        public static Object isSealed(ExecutionContext cx, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            return Type.objectValue(o).hasIntegrity(cx, IntegrityLevel.Sealed);
        }

        /**
         * 15.2.3.12 Object.isFrozen ( O )
         */
        @Function(name = "isFrozen", arity = 1)
        public static Object isFrozen(ExecutionContext cx, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            return Type.objectValue(o).hasIntegrity(cx, IntegrityLevel.Frozen);
        }

        /**
         * 15.2.3.13 Object.isExtensible ( O )
         */
        @Function(name = "isExtensible", arity = 1)
        public static Object isExtensible(ExecutionContext cx, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            return IsExtensible(cx, Type.objectValue(o));
        }

        /**
         * 15.2.3.14 Object.keys ( O )
         */
        @Function(name = "keys", arity = 1)
        public static Object keys(ExecutionContext cx, Object thisValue, Object o) {
            // FIXME: spec bug - steps start at 8
            if (!Type.isObject(o)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            List<String> nameList = GetOwnPropertyKeys(cx, Type.objectValue(o));
            return CreateArrayFromList(cx, nameList);
        }

        /**
         * 15.2.3.15 Object.getOwnPropertyKeys ( O )
         */
        @Function(name = "getOwnPropertyKeys", arity = 1)
        public static Object getOwnPropertyKeys(ExecutionContext cx, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            return Type.objectValue(o).ownPropertyKeys(cx);
        }

        /**
         * 15.2.3.16 Object.is ( value1, value2 )
         */
        @Function(name = "is", arity = 2)
        public static Object is(ExecutionContext cx, Object thisValue, Object value1, Object value2) {
            return SameValue(value1, value2);
        }

        /**
         * 15.2.3.17 Object.assign ( target, source )
         */
        @Function(name = "assign", arity = 2)
        public static Object assign(ExecutionContext cx, Object thisValue, Object target,
                Object source) {
            if (!Type.isObject(target)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            if (!Type.isObject(source)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject _target = Type.objectValue(target);
            ScriptObject _source = Type.objectValue(source);
            ScriptException pendingException = null;
            List<Object> keys = GetOwnEnumerableKeys(cx, _source);
            for (Object key : keys) {
                if (key instanceof String) {
                    String ownKey = (String) key;
                    Object value = Get(cx, _source, ownKey);
                    if (isSuperBoundTo(value, _source)) {
                        value = superBindTo(cx, value, _target);
                    }
                    try {
                        Put(cx, _target, ownKey, value, true);
                    } catch (ScriptException e) {
                        if (pendingException == null) {
                            pendingException = e;
                        }
                    }
                } else {
                    assert key instanceof Symbol;
                    Symbol ownKey = (Symbol) key;
                    Object value = Get(cx, _source, ownKey);
                    if (isSuperBoundTo(value, _source)) {
                        value = superBindTo(cx, value, _target);
                    }
                    try {
                        Put(cx, _target, ownKey, value, true);
                    } catch (ScriptException e) {
                        if (pendingException == null) {
                            pendingException = e;
                        }
                    }
                }
            }
            if (pendingException != null) {
                throw pendingException;
            }
            return _target;
        }

        /**
         * 15.2.3.18 Object.mixin ( target, source )
         */
        @Function(name = "mixin", arity = 2)
        public static Object mixin(ExecutionContext cx, Object thisValue, Object target,
                Object source) {
            if (!Type.isObject(target)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            if (!Type.isObject(source)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject _target = Type.objectValue(target);
            ScriptObject _source = Type.objectValue(source);
            ScriptException pendingException = null;
            List<Object> keys = GetOwnEnumerableKeys(cx, _source);
            for (Object key : keys) {
                if (key instanceof String) {
                    String ownKey = (String) key;
                    Property desc = _source.getOwnProperty(cx, ownKey);
                    if (desc != null) {
                        try {
                            PropertyDescriptor desc2 = fromDescriptor(cx,
                                    desc.toPropertyDescriptor(), _source, _target);
                            DefinePropertyOrThrow(cx, _target, ownKey, desc2);
                        } catch (ScriptException e) {
                            if (pendingException == null) {
                                pendingException = e;
                            }
                        }
                    }
                } else {
                    assert key instanceof Symbol;
                    Symbol ownKey = (Symbol) key;
                    Property desc = _source.getOwnProperty(cx, ownKey);
                    if (desc != null) {
                        try {
                            PropertyDescriptor desc2 = fromDescriptor(cx,
                                    desc.toPropertyDescriptor(), _source, _target);
                            DefinePropertyOrThrow(cx, _target, ownKey, desc2);
                        } catch (ScriptException e) {
                            if (pendingException == null) {
                                pendingException = e;
                            }
                        }
                    }
                }
            }
            if (pendingException != null) {
                throw pendingException;
            }
            return _target;
        }
    }

    /**
     * 15.2.3.7 Object.defineProperties ( O, Properties )
     * <p>
     * Runtime Semantics: ObjectDefineProperties Abstract Operation
     */
    public static ScriptObject ObjectDefineProperties(ExecutionContext cx, Object o,
            Object properties) {
        if (!Type.isObject(o)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        ScriptObject obj = Type.objectValue(o);
        ScriptObject props = ToObject(cx, properties);
        // FIXME: spec bug ('keys of each enumerable own property' -> string/symbol/private ?)
        List<String> names = GetOwnPropertyKeys(cx, props);
        List<PropertyDescriptor> descriptors = new ArrayList<>();
        for (String p : names) {
            Object descObj = Get(cx, props, p);
            PropertyDescriptor desc = ToPropertyDescriptor(cx, descObj);
            descriptors.add(desc);
        }
        ScriptException pendingException = null;
        for (int i = 0, size = names.size(); i < size; ++i) {
            String p = names.get(i);
            PropertyDescriptor desc = descriptors.get(i);
            try {
                DefinePropertyOrThrow(cx, obj, p, desc);
            } catch (ScriptException e) {
                if (pendingException == null) {
                    pendingException = e;
                }
            }
        }
        if (pendingException != null) {
            throw pendingException;
        }
        return obj;
    }

    /**
     * Returns a list of all enumerable, non-private own property keys
     */
    private static List<Object> GetOwnEnumerableKeys(ExecutionContext cx, ScriptObject object) {
        List<Object> ownKeys = new ArrayList<>();
        Iterator<?> keys = FromListIterator(cx, object.ownPropertyKeys(cx));
        while (keys.hasNext()) {
            Object key = ToPropertyKey(cx, keys.next());
            if (key instanceof String) {
                String ownKey = (String) key;
                Property desc = object.getOwnProperty(cx, ownKey);
                if (desc != null && desc.isEnumerable()) {
                    ownKeys.add(ownKey);
                }
            } else {
                assert key instanceof Symbol;
                Symbol ownKey = (Symbol) key;
                if (!ownKey.isPrivate()) {
                    Property desc = object.getOwnProperty(cx, ownKey);
                    if (desc != null && desc.isEnumerable()) {
                        ownKeys.add(ownKey);
                    }
                }
            }
        }
        return ownKeys;
    }

    /**
     * Returns {@code desc} with [[Value]] resp. [[Get]] and [[Set]] super-rebound from
     * {@code source} to {@code target}
     */
    private static PropertyDescriptor fromDescriptor(ExecutionContext cx, PropertyDescriptor desc,
            ScriptObject source, ScriptObject target) {
        if (desc.isDataDescriptor()) {
            Object value = desc.getValue();
            if (isSuperBoundTo(value, source)) {
                desc.setValue(superBindTo(cx, value, target));
            }
        } else {
            assert desc.isAccessorDescriptor();
            Callable getter = desc.getGetter();
            if (isSuperBoundTo(getter, source)) {
                desc.setGetter(superBindTo(cx, getter, target));
            }
            Callable setter = desc.getSetter();
            if (isSuperBoundTo(setter, source)) {
                desc.setSetter(superBindTo(cx, setter, target));
            }
        }
        return desc;
    }

    /**
     * Returns <code>true</code> if {@code value} is super-bound to {@code source}
     */
    private static boolean isSuperBoundTo(Object value, ScriptObject source) {
        if (value instanceof OrdinaryFunction) {
            ScriptObject homeObject = ((OrdinaryFunction) value).getHomeObject();
            return (homeObject == source);
        }
        if (value instanceof OrdinaryGenerator) {
            ScriptObject homeObject = ((OrdinaryGenerator) value).getHomeObject();
            return (homeObject == source);
        }
        return false;
    }

    /**
     * Super-binds {@code value} to {@code target}
     */
    private static Callable superBindTo(ExecutionContext cx, Object value, ScriptObject target) {
        if (value instanceof OrdinaryGenerator) {
            OrdinaryGenerator gen = (OrdinaryGenerator) value;
            assert gen.isInitialised() : "uninitialised function object";
            return GeneratorFunctionCreate(cx, gen.getFunctionKind(), gen.getFunction(),
                    gen.getScope(), gen.getPrototype(cx), target, gen.getMethodName());
        } else {
            assert value instanceof OrdinaryFunction;
            OrdinaryFunction fn = (OrdinaryFunction) value;
            assert fn.isInitialised() : "uninitialised function object";
            return FunctionCreate(cx, fn.getFunctionKind(), fn.getFunction(), fn.getScope(),
                    fn.getPrototype(cx), target, fn.getMethodName());
        }
    }
}
