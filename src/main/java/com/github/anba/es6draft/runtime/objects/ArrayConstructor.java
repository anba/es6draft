/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwRangeError;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.4 Array Objects</h2>
 * <ul>
 * <li>15.4.1 The Array Constructor Called as a Function
 * <li>15.4.2 The Array Constructor
 * <li>15.4.3 Properties of the Array Constructor
 * </ul>
 */
public class ArrayConstructor extends OrdinaryObject implements Scriptable, Callable, Constructor,
        Initialisable {
    public ArrayConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
        AddRestrictedFunctionProperties(realm, this);
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinFunction;
    }

    @Override
    public String toSource() {
        return "function Array() { /* native code */ }";
    }

    /**
     * 15.4.1.1 Array ( [ item1 [ , item2 [ , ... ] ] ] )
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        return construct(args);
    }

    /**
     * 15.4.2.1 new Array ( [ item0 [ , item1 [ , ... ] ] ] )<br>
     * 15.4.2.2 new Array (len)
     */
    @Override
    public Object construct(Object... args) {
        if (args.length != 1) {
            // [15.4.2.1]
            /* step 1 */
            int len = args.length;
            /* step 2-3 */
            Scriptable array = ArrayCreate(realm(), len);
            /* step 4-6 */
            for (int k = 0; k < len; ++k) {
                String pk = ToString(k);
                Object itemK = args[k];
                array.defineOwnProperty(pk, new PropertyDescriptor(itemK, true, true, true));
            }
            /* step 7-8 */
            Put(realm(), array, "length", len, true);
            /* step 9 */
            return array;
        } else {
            // [15.4.2.2]
            Object len = args[0];
            /* step 1 */
            if (!Type.isNumber(len)) {
                Scriptable array = ArrayCreate(realm(), 1);
                DefinePropertyOrThrow(realm(), array, "0", new PropertyDescriptor(len, true, true,
                        true));
                return array;
            }
            /* step 2 */
            long intLen = ToUint32(realm(), len);
            /* step 3 */
            if (intLen != Type.numberValue(len)) {
                throw throwRangeError(realm(), "");
            }
            /* step 4 */
            Scriptable array = ArrayCreate(realm(), intLen);
            /* step 5 */
            return array;
        }
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
        public static Object isArray(Realm realm, Object thisValue, Object arg) {
            /* step 1 */
            if (!Type.isObject(arg)) {
                return false;
            }
            /* step 2 */
            if (Type.objectValue(arg).getBuiltinBrand() == BuiltinBrand.BuiltinArray) {
                return true;
            }
            /* step 3 */
            return false;
        }

        /**
         * 15.4.3.3 Array.of ( ...items )
         */
        @Function(name = "of", arity = 0)
        public static Object of(Realm realm, Object thisValue, Object... items) {
            /* step 1-2 */
            int len = items.length;
            /* step 3 */
            Object c = thisValue;
            Scriptable a;
            if (IsConstructor(c)) {
                /* step 4, 6 */
                Object newObj = ((Constructor) c).construct(len);
                a = ToObject(realm, newObj);
            } else {
                /* step 5, 6 */
                a = ArrayCreate(realm, len);
            }
            /* step 7-8 */
            for (int k = 0; k < len; ++k) {
                String pk = ToString(k);
                Object kValue = items[k];
                DefinePropertyOrThrow(realm, a, pk,
                        new PropertyDescriptor(kValue, true, true, true));
            }
            /* step 9-10 */
            Put(realm, a, "length", len, true);
            /* step 11 */
            return a;
        }

        /**
         * 15.4.3.4 Array.from ( arrayLike )
         */
        @Function(name = "from", arity = 1)
        public static Object from(Realm realm, Object thisValue, Object arrayLike) {
            /* step 1-2 */
            Scriptable items = ToObject(realm, arrayLike);
            /* step 3 */
            Object lenValue = Get(items, "length");
            /* step 4-5 */
            double len = ToInteger(realm, lenValue);
            long llen = (long) len;
            /* step 6 */
            Object c = thisValue;
            Scriptable a;
            if (IsConstructor(c)) {
                /* step 7, 9 */
                Object newObj = ((Constructor) c).construct(len);
                a = ToObject(realm, newObj);
            } else {
                /* step 8, 9 */
                a = ArrayCreate(realm, llen);
            }
            /* step 10-11 */
            for (long k = 0; k < llen; ++k) {
                String pk = ToString(k);
                boolean kPresent = HasProperty(items, pk);
                if (kPresent) {
                    Object kValue = Get(items, pk);
                    // FIXME: spec bug (Bug 1139)
                    DefinePropertyOrThrow(realm, a, pk, new PropertyDescriptor(kValue, true, true,
                            true));
                }
            }
            /* step 12-13 */
            Put(realm, a, "length", len, true);
            /* step 14 */
            return a;
        }

        /**
         * TODO: not yet in spec
         */
        @Function(name = "@@create", symbol = BuiltinSymbol.create, arity = 0)
        public static Object create(Realm realm, Object thisValue) {
            Scriptable obj = OrdinaryCreateFromConstructor(realm, thisValue,
                    Intrinsics.ArrayPrototype);
            return obj;
        }
    }
}
