/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.7 Number Objects</h2>
 * <ul>
 * <li>15.7.1 The Number Constructor Called as a Function
 * <li>15.7.2 The Number Constructor
 * <li>15.7.3 Properties of the Number Constructor
 * </ul>
 */
public class NumberConstructor extends OrdinaryObject implements Scriptable, Callable, Constructor,
        Initialisable {
    public NumberConstructor(Realm realm) {
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
        return "function Number() { /* native code */ }";
    }

    /**
     * 15.7.1.1 Number ( [ value ] )
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        if (args.length > 0) {
            return ToNumber(realm(), args[0]);
        }
        return +0.0;
    }

    /**
     * 15.7.2.1 new Number ( [ value ] )
     */
    @Override
    public Object construct(Object... args) {
        double numberData = args.length > 0 ? ToNumber(realm(), args[0]) : +0.0;
        NumberObject obj = new NumberObject(realm(), numberData);
        obj.setPrototype(realm().getIntrinsic(Intrinsics.NumberPrototype));
        return obj;
    }

    /**
     * 15.7.3 Properties of the Number Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        /**
         * 15.7.3.1 Number.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.NumberPrototype;

        /**
         * 15.7.3.2 Number.MAX_VALUE
         */
        @Value(name = "MAX_VALUE", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double MAX_VALUE = Double.MAX_VALUE;

        /**
         * 15.7.3.3 Number.MIN_VALUE
         */
        @Value(name = "MIN_VALUE", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double MIN_VALUE = Double.MIN_VALUE;

        /**
         * 15.7.3.4 Number.NaN
         */
        @Value(name = "NaN", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double NaN = Double.NaN;

        /**
         * 15.7.3.5 Number.NEGATIVE_INFINITY
         */
        @Value(name = "NEGATIVE_INFINITY", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final Double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;

        /**
         * 15.7.3.6 Number.POSITIVE_INFINITY
         */
        @Value(name = "POSITIVE_INFINITY", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final Double POSITIVE_INFINITY = Double.POSITIVE_INFINITY;

        /**
         * 15.7.3.7 Number.EPSILON
         */
        @Value(name = "EPSILON", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double EPSILON = Double.MIN_NORMAL;

        /**
         * 15.7.3.8 Number.MAX_INTEGER
         */
        @Value(name = "MAX_INTEGER", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double MAX_INTEGER = (double) 0x1FFFFFFFFFFFFFL;

        /**
         * 15.7.3.9 Number.parseInt (string, radix)
         */
        @Function(name = "parseInt", arity = 2)
        public static Object parseInt(Realm realm, Object thisValue, Object string, Object radix) {
            // TODO: implement
            assert false : "NYI";
            return 0;
        }

        /**
         * 15.7.3.10 Number.parseFloat (string)
         */
        @Function(name = "parseFloat", arity = 1)
        public static Object parseFloat(Realm realm, Object thisValue, Object string) {
            // TODO: implement
            assert false : "NYI";
            return 0;
        }

        /**
         * 15.7.3.11 Number.isNaN (number)
         */
        @Function(name = "isNaN", arity = 1)
        public static Object isNaN(Realm realm, Object thisValue, Object number) {
            if (!Type.isNumber(number)) {
                return false;
            }
            return Double.isNaN(Type.numberValue(number));
        }

        /**
         * 15.7.3.12 Number.isFinite (number)
         */
        @Function(name = "isFinite", arity = 1)
        public static Object isFinite(Realm realm, Object thisValue, Object number) {
            if (!Type.isNumber(number)) {
                return false;
            }
            double num = Type.numberValue(number);
            return !(Double.isInfinite(num) || Double.isNaN(num));
        }

        /**
         * 15.7.3.13 Number.isInteger (number)
         */
        @Function(name = "isInteger", arity = 1)
        public static Object isInteger(Realm realm, Object thisValue, Object number) {
            if (!Type.isNumber(number)) {
                return false;
            }
            return ToInteger(realm, number) == Type.numberValue(number);
        }

        /**
         * 15.7.3.14 Number.toInt (number)
         */
        @Function(name = "toInt", arity = 1)
        public static Object toInt(Realm realm, Object thisValue, Object number) {
            return ToInteger(realm, number);
        }
    }
}
