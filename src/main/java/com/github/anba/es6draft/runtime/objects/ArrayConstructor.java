/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

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
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.1 Array Objects</h2>
 * <ul>
 * <li>22.1.1 The Array Constructor
 * <li>22.1.2 Properties of the Array Constructor
 * </ul>
 */
public class ArrayConstructor extends BuiltinConstructor implements Initialisable {
    public ArrayConstructor(Realm realm) {
        super(realm, "Array");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 22.1.1.1 Array ( [ item1 [ , item2 [ , ... ] ] ] )<br>
     * 22.1.1.2 Array (len)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        int numberOfArgs = args.length;
        if (numberOfArgs != 1) {
            // [22.1.1.1]
            /* steps 1-6 */
            ExoticArray array = initOrCreateArray(calleeContext, thisValue, numberOfArgs);
            /* steps 7-9 */
            for (int k = 0; k < numberOfArgs; ++k) {
                String pk = ToString(k);
                Object itemK = args[k];
                CreateDataPropertyOrThrow(calleeContext, array, pk, itemK);
            }
            /* steps 10-11 */
            Put(calleeContext, array, "length", numberOfArgs, true);
            /* step 12 */
            return array;
        } else {
            // [22.1.1.2]
            /* steps 1-6 */
            ExoticArray array = initOrCreateArray(calleeContext, thisValue, 0);
            Object len = args[0];
            /* steps 7-8 */
            long intLen;
            if (!Type.isNumber(len)) {
                CreateDataPropertyOrThrow(calleeContext, array, "0", len);
                intLen = 1;
            } else {
                double llen = Type.numberValue(len);
                intLen = ToUint32(llen);
                if (intLen != llen) {
                    throw newRangeError(calleeContext, Messages.Key.InvalidArrayLength);
                }
            }
            /* steps 9-10 */
            Put(calleeContext, array, "length", intLen, true);
            /* step 11 */
            return array;
        }
    }

    private ExoticArray initOrCreateArray(ExecutionContext cx, Object thisValue, int length) {
        /* [22.1.1.1] steps 3-6 */
        /* [22.1.1.2] steps 3-6 */
        if (thisValue instanceof ExoticArray) {
            ExoticArray array = (ExoticArray) thisValue;
            if (!array.getInitialisationState()) {
                array.setInitialisationState(true);
                return array;
            }
        }
        ScriptObject proto = GetPrototypeFromConstructor(cx, this, Intrinsics.ArrayPrototype);
        return ArrayCreate(cx, length, proto);
    }

    /**
     * 22.1.1.3 new Array ( ... argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    /**
     * 22.1.2 Properties of the Array Constructor
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
        public static final String name = "Array";

        /**
         * 22.1.2.4 Array.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.ArrayPrototype;

        /**
         * 22.1.2.2 Array.isArray ( arg )
         */
        @Function(name = "isArray", arity = 1)
        public static Object isArray(ExecutionContext cx, Object thisValue, Object arg) {
            /* steps 1-3 */
            return arg instanceof ExoticArray;
        }

        /**
         * 22.1.2.3 Array.of ( ...items )
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
                a = ((Constructor) c).construct(cx, len);
            } else {
                a = ArrayCreate(cx, len);
            }
            /* steps 7-8 */
            for (int k = 0; k < len; ++k) {
                String pk = ToString(k);
                Object kValue = items[k];
                CreateDataPropertyOrThrow(cx, a, pk, kValue);
            }
            /* steps 9-10 */
            Put(cx, a, "length", len, true);
            /* step 11 */
            return a;
        }

        /**
         * 22.1.2.1 Array.from ( arrayLike, mapfn=undefined, thisArg=undefined )
         */
        @Function(name = "from", arity = 1)
        public static Object from(ExecutionContext cx, Object thisValue, Object arrayLike,
                Object mapfn, Object thisArg) {
            /* step 1 */
            Object c = thisValue;
            /* steps 2-3 */
            ScriptObject items = ToObject(cx, arrayLike);
            /* steps 4-5 */
            Callable mapper = null;
            boolean mapping;
            if (Type.isUndefined(mapfn)) {
                mapping = false;
            } else {
                if (!IsCallable(mapfn)) {
                    throw newTypeError(cx, Messages.Key.NotCallable);
                }
                mapping = true;
                mapper = (Callable) mapfn;
            }
            /* steps 6-7 */
            boolean usingIterator = HasProperty(cx, items, BuiltinSymbol.iterator.get());
            /* step 8 */
            if (usingIterator) {
                /* steps 8a-8c */
                ScriptObject a;
                if (IsConstructor(c)) {
                    a = ((Constructor) c).construct(cx);
                } else {
                    a = ArrayCreate(cx, 0);
                }
                /* steps 8d-8e */
                ScriptObject iterator = GetIterator(cx, items);
                /* steps 8f-8g */
                for (int k = 0;; ++k) {
                    String pk = ToString(k);
                    ScriptObject next = IteratorStep(cx, iterator);
                    if (next == null) {
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
                    CreateDataPropertyOrThrow(cx, a, pk, mappedValue);
                }
            }
            /* step 9 (?) */
            /* step 10 */
            Object lenValue = Get(cx, items, "length");
            /* steps 11-12 */
            long len = ToLength(cx, lenValue);
            /* steps 13-15 */
            ScriptObject a;
            if (IsConstructor(c)) {
                a = ((Constructor) c).construct(cx, len);
            } else {
                a = ArrayCreate(cx, len);
            }
            /* steps 16-17 */
            for (long k = 0; k < len; ++k) {
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
                    CreateDataPropertyOrThrow(cx, a, pk, mappedValue);
                }
            }
            /* steps 18-19 */
            Put(cx, a, "length", len, true);
            /* step 20 */
            return a;
        }

        /**
         * 22.1.2.5 Array[ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            ScriptObject proto = GetPrototypeFromConstructor(cx, thisValue,
                    Intrinsics.ArrayPrototype);
            /* steps 4-5 */
            return ArrayCreate(cx, proto);
        }
    }
}
