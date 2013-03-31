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
 * <li>15.4.1 The Array Constructor Called as a Function
 * <li>15.4.2 The Array Constructor
 * <li>15.4.3 Properties of the Array Constructor
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
        int numberOfArgs = args.length;
        if (numberOfArgs != 1) {
            // [15.4.1.1]
            ExoticArray array = maybeCreateArray(callerContext, thisValue, numberOfArgs);
            for (int k = 0; k < numberOfArgs; ++k) {
                String pk = ToString(k);
                Object itemK = args[k];
                DefinePropertyOrThrow(callerContext, array, pk, new PropertyDescriptor(itemK, true,
                        true, true));
            }
            Put(callerContext, array, "length", numberOfArgs, true);
            return array;
        } else {
            // [15.4.1.2]
            ExoticArray array = maybeCreateArray(callerContext, thisValue, 0);
            Object len = args[0];
            long intLen;
            if (!Type.isNumber(len)) {
                DefinePropertyOrThrow(callerContext, array, "0", new PropertyDescriptor(len, true,
                        true, true));
                intLen = 1;
            } else {
                intLen = ToUint32(callerContext, len);
                if (intLen != Type.numberValue(len)) {
                    throw throwRangeError(callerContext, Messages.Key.InvalidArrayLength);
                }
            }
            Put(callerContext, array, "length", intLen, true);
            return array;
        }
    }

    private ExoticArray maybeCreateArray(ExecutionContext cx, Object thisValue, long length) {
        if (thisValue instanceof ExoticArray) {
            ExoticArray array = (ExoticArray) thisValue;
            if (!array.getArrayInitializationState()) {
                array.setArrayInitializationState(true);
                return array;
            }
        }
        ScriptObject proto = GetPrototypeFromConstructor(cx, this, Intrinsics.ArrayPrototype);
        return ArrayCreate(cx, length, proto);
    }

    /**
     * 15.4.2 The Array Constructor
     */
    @Override
    public Object construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * 15.4.3 Properties of the Array Constructor
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
         * 15.4.3.1 Array.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.ArrayPrototype;

        /**
         * 15.4.3.2 Array.isArray ( arg )
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
         * 15.4.3.3 Array.of ( ...items )
         */
        @Function(name = "of", arity = 0)
        public static Object of(ExecutionContext cx, Object thisValue, Object... items) {
            /* step 1-2 */
            int len = items.length;
            /* step 3 */
            Object c = thisValue;
            ScriptObject a;
            if (IsConstructor(c)) {
                /* step 4, 6 */
                Object newObj = ((Constructor) c).construct(cx, len);
                a = ToObject(cx, newObj);
            } else {
                /* step 5, 6 */
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
         * 15.4.3.4 Array.from ( arrayLike, mapfn=undefined, thisArg=undefined )
         */
        @Function(name = "from", arity = 1)
        public static Object from(ExecutionContext cx, Object thisValue, Object arrayLike,
                Object mapfn, Object thisArg) {
            /* step 1-2 */
            ScriptObject items = ToObject(cx, arrayLike);
            // FIXME: spec bug (mapfn and thisArg unused)
            Callable mapper = null;
            if (!Type.isUndefined(mapfn)) {
                if (!IsCallable(mapfn)) {
                    throw throwTypeError(cx, Messages.Key.NotCallable);
                }
                mapper = (Callable) mapfn;
            }
            /* step 3 */
            Object lenValue = Get(cx, items, "length");
            /* step 4-5 */
            double len = ToInteger(cx, lenValue);
            long llen = (long) len;
            /* step 6 */
            Object c = thisValue;
            ScriptObject a;
            if (IsConstructor(c)) {
                /* step 7, 9 */
                Object newObj = ((Constructor) c).construct(cx, len);
                a = ToObject(cx, newObj);
            } else {
                /* step 8, 9 */
                a = ArrayCreate(cx, llen);
            }
            /* step 10-11 */
            for (long k = 0; k < llen; ++k) {
                String pk = ToString(k);
                boolean kPresent = HasProperty(cx, items, pk);
                if (kPresent) {
                    Object kValue = Get(cx, items, pk);
                    if (mapper != null) {
                        kValue = mapper.call(cx, thisArg, kValue);
                    }
                    // FIXME: spec bug (Bug 1139)
                    DefinePropertyOrThrow(cx, a, pk, new PropertyDescriptor(kValue, true, true,
                            true));
                }
            }
            /* step 12-13 */
            Put(cx, a, "length", len, true);
            /* step 14 */
            return a;
        }

        /**
         * 15.4.3.5 Array[ @@create ] ( )
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(ExecutionContext cx, Object thisValue) {
            ScriptObject proto = GetPrototypeFromConstructor(cx, thisValue,
                    Intrinsics.ArrayPrototype);
            return ArrayCreate(cx, -1, proto);
        }
    }
}
