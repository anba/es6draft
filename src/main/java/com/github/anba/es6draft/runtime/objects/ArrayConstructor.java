/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.GetIterator;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.IteratorComplete;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.IteratorNext;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.IteratorValue;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.4 Array Objects</h2>
 * <ul>
 * <li>15.4.1 The Array Constructor
 * <li>15.4.2 Properties of the Array Constructor
 * </ul>
 */
public class ArrayConstructor extends BuiltinFunction implements Constructor, Initialisable {
    public ArrayConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 15.4.1.1 Array ( [ item1 [ , item2 [ , ... ] ] ] )<br>
     * 15.4.1.2 Array (len)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        int numberOfArgs = args.length;
        if (numberOfArgs != 1) {
            // [15.4.1.1]
            /* steps 1-6 */
            ExoticArray array = initOrCreateArray(calleeContext, thisValue, numberOfArgs);
            /* steps 7-9 */
            for (int k = 0; k < numberOfArgs; ++k) {
                String pk = ToString(k);
                Object itemK = args[k];
                DefinePropertyOrThrow(calleeContext, array, pk, new PropertyDescriptor(itemK, true,
                        true, true));
            }
            /* steps 10-11 */
            Put(calleeContext, array, "length", numberOfArgs, true);
            /* step 12 */
            return array;
        } else {
            // [15.4.1.2]
            /* steps 1-6 */
            ExoticArray array = initOrCreateArray(calleeContext, thisValue, 0);
            Object len = args[0];
            /* steps 7-8 */
            long intLen;
            if (!Type.isNumber(len)) {
                DefinePropertyOrThrow(calleeContext, array, "0", new PropertyDescriptor(len, true,
                        true, true));
                intLen = 1;
            } else {
                intLen = ToUint32(calleeContext, len);
                if (intLen != Type.numberValue(len)) {
                    throw throwRangeError(calleeContext, Messages.Key.InvalidArrayLength);
                }
            }
            /* steps 9-10 */
            Put(calleeContext, array, "length", intLen, true);
            /* step 11 */
            return array;
        }
    }

    private ExoticArray initOrCreateArray(ExecutionContext cx, Object thisValue, int length) {
        /* [15.4.1.1] steps 3-6 */
        /* [15.4.1.2] steps 3-6 */
        if (thisValue instanceof ExoticArray) {
            ExoticArray array = (ExoticArray) thisValue;
            if (!array.getArrayInitialisationState()) {
                array.setArrayInitialisationState(true);
                return array;
            }
        }
        ScriptObject proto = GetPrototypeFromConstructor(cx, this, Intrinsics.ArrayPrototype);
        return ArrayCreate(cx, length, proto);
    }

    /**
     * 15.4.1.3 new Array ( ... argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * 15.4.2 Properties of the Array Constructor
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
        public static final String name = "Array";

        /**
         * 15.4.2.1 Array.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.ArrayPrototype;

        /**
         * 15.4.2.2 Array.isArray ( arg )
         */
        @Function(name = "isArray", arity = 1)
        public static Object isArray(ExecutionContext cx, Object thisValue, Object arg) {
            /* step 1 */
            if (!Type.isObject(arg)) {
                return false;
            }
            /* step 2 */
            if (arg instanceof ExoticArray) {
                return true;
            }
            /* step 3 */
            return false;
        }

        /**
         * 15.4.2.3 Array.of ( ...items )
         */
        @Function(name = "of", arity = 0)
        public static Object of(ExecutionContext cx, Object thisValue, Object... items) {
            /* steps 1-2 */
            int len = items.length;
            /* step 3 */
            Object c = thisValue;
            /* steps 4-6 */
            ScriptObject a;
            if (IsConstructor(c)) {
                ScriptObject newObj = ((Constructor) c).construct(cx, len);
                a = ToObject(cx, newObj);
            } else {
                a = ArrayCreate(cx, len);
            }
            /* step 7-8 */
            for (int k = 0; k < len; ++k) {
                String pk = ToString(k);
                Object kValue = items[k];
                DefinePropertyOrThrow(cx, a, pk, new PropertyDescriptor(kValue, true, true, true));
            }
            /* step 9-10 */
            Put(cx, a, "length", len, true);
            /* step 11 */
            return a;
        }

        /**
         * 15.4.2.4 Array.from ( arrayLike, mapfn=undefined, thisArg=undefined )
         */
        @Function(name = "from", arity = 1)
        public static Object from(ExecutionContext cx, Object thisValue, Object arrayLike,
                Object mapfn, Object thisArg) {
            /* step 1 */
            Object c = thisValue;
            /* step 2-3 */
            ScriptObject items = ToObject(cx, arrayLike);
            /* step 4-5 */
            Callable mapper = null;
            boolean mapping;
            if (Type.isUndefined(mapfn)) {
                mapping = false;
            } else {
                if (!IsCallable(mapfn)) {
                    throw throwTypeError(cx, Messages.Key.NotCallable);
                }
                mapping = true;
                mapper = (Callable) mapfn;
            }
            /* step 6-7 */
            boolean usingIterator = HasProperty(cx, items, BuiltinSymbol.iterator.get());
            /* step 8 */
            if (usingIterator) {
                /* steps 8a-8b */
                ScriptObject iterator = GetIterator(cx, items);
                /* steps 8c-8e */
                ScriptObject a;
                if (IsConstructor(c)) {
                    ScriptObject newObj = ((Constructor) c).construct(cx);
                    a = ToObject(cx, newObj);
                } else {
                    a = ArrayCreate(cx, 0);
                }
                /* steps 8f-8h */
                for (int k = 0;; ++k) {
                    String pk = ToString(k);
                    ScriptObject next = IteratorNext(cx, iterator);
                    boolean done = IteratorComplete(cx, next);
                    if (done) {
                        Put(cx, a, "length", k, true);
                        return a;
                    }
                    Object nextValue = IteratorValue(cx, next);
                    Object mappedValue;
                    if (mapping) {
                        mappedValue = mapper.call(cx, thisArg, nextValue);
                    } else {
                        mappedValue = nextValue;
                    }
                    DefinePropertyOrThrow(cx, a, pk, new PropertyDescriptor(mappedValue, true,
                            true, true));
                }
            }
            /* step 9 (?) */
            /* step 10 */
            Object lenValue = Get(cx, items, "length");
            /* step 11-12 */
            double len = ToInteger(cx, lenValue);
            /* step 13-15 */
            ScriptObject a;
            if (IsConstructor(c)) {
                ScriptObject newObj = ((Constructor) c).construct(cx, len);
                a = ToObject(cx, newObj);
            } else {
                long arrayLen = ToUint32(len);
                if (arrayLen != len) {
                    throw throwRangeError(cx, Messages.Key.InvalidArrayLength);
                }
                a = ArrayCreate(cx, arrayLen);
            }
            /* step 16-17 */
            long llen = (long) len;
            for (long k = 0; k < llen; ++k) {
                String pk = ToString(k);
                boolean kPresent = HasProperty(cx, items, pk);
                if (kPresent) {
                    Object kValue = Get(cx, items, pk);
                    Object mappedValue;
                    if (mapping) {
                        mappedValue = mapper.call(cx, thisArg, kValue, k, items);
                    } else {
                        mappedValue = kValue;
                    }
                    DefinePropertyOrThrow(cx, a, pk, new PropertyDescriptor(mappedValue, true,
                            true, true));
                }
            }
            /* step 18-19 */
            Put(cx, a, "length", len, true);
            /* step 20 */
            return a;
        }

        /**
         * 15.4.2.5 Array[ @@create ] ( )
         */
        @Function(name = "@@create", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            ScriptObject proto = GetPrototypeFromConstructor(cx, thisValue,
                    Intrinsics.ArrayPrototype);
            /* steps 4-5 */
            return ArrayCreate(cx, -1, proto);
        }
    }
}
