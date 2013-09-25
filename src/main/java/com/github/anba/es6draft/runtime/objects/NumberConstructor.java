/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import org.mozilla.javascript.StringToNumber;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.7 Number Objects</h2>
 * <ul>
 * <li>15.7.1 The Number Constructor
 * <li>15.7.2 Properties of the Number Constructor
 * </ul>
 */
public class NumberConstructor extends BuiltinConstructor implements Initialisable {
    public NumberConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 15.7.1.1 Number ( [ value ] )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* step 1 (omitted) */
        /* steps 2-4 */
        double n = (args.length > 0 ? ToNumber(calleeContext, args[0]) : +0.0);
        /* step 5 */
        if (thisValue instanceof NumberObject) {
            NumberObject obj = (NumberObject) thisValue;
            if (!obj.isInitialised()) {
                obj.setNumberData(n);
                return obj;
            }
        }
        /* step 6 */
        return n;
    }

    /**
     * 15.7.1.2 new Number (...argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * 15.7.2 Properties of the Number Constructor
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
        public static final String name = "Number";

        /**
         * 15.7.2.1 Number.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.NumberPrototype;

        /**
         * 15.7.2.2 Number.MAX_VALUE
         */
        @Value(name = "MAX_VALUE", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double MAX_VALUE = Double.MAX_VALUE;

        /**
         * 15.7.2.3 Number.MIN_VALUE
         */
        @Value(name = "MIN_VALUE", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double MIN_VALUE = Double.MIN_VALUE;

        /**
         * 15.7.2.4 Number.NaN
         */
        @Value(name = "NaN", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double NaN = Double.NaN;

        /**
         * 15.7.2.5 Number.NEGATIVE_INFINITY
         */
        @Value(name = "NEGATIVE_INFINITY", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final Double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;

        /**
         * 15.7.2.6 Number.POSITIVE_INFINITY
         */
        @Value(name = "POSITIVE_INFINITY", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final Double POSITIVE_INFINITY = Double.POSITIVE_INFINITY;

        /**
         * 15.7.2.7 Number.EPSILON
         */
        @Value(name = "EPSILON", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double EPSILON = Math.ulp(1.0);

        /**
         * 15.7.2.8 Number.MAX_SAFE_INTEGER
         */
        @Value(name = "MAX_SAFE_INTEGER", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final Double MAX_SAFE_INTEGER = (double) 0x1FFFFFFFFFFFFFL;

        /**
         * 15.7.2.x Number.MIN_SAFE_INTEGER
         */
        @Value(name = "MIN_SAFE_INTEGER", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final Double MIN_SAFE_INTEGER = (double) -0x1FFFFFFFFFFFFFL;

        /**
         * 15.7.2.9 Number.parseInt (string, radix)
         */
        @Function(name = "parseInt", arity = 2)
        public static Object parseInt(ExecutionContext cx, Object thisValue, Object string,
                Object radix) {
            /* steps 1-2 */
            String inputString = ToFlatString(cx, string);
            /* step 3 */
            String s = Strings.trimLeft(inputString);
            int len = s.length();
            int index = 0;
            /* steps 4-6 */
            boolean isPos = true;
            if (index < len && (s.charAt(index) == '+' || s.charAt(index) == '-')) {
                isPos = (s.charAt(index) == '+');
                index += 1;
            }
            /* steps 7-8 */
            int r = ToInt32(cx, radix);
            /* step 9 */
            boolean stripPrefix = true;
            if (r != 0) {
                /* step 10 */
                if (r < 2 || r > 36) {
                    return Double.NaN;
                }
                stripPrefix = (r == 16);
            } else {
                /* step 11 */
                r = 10;
            }
            /* step 12 */
            if (stripPrefix && index + 1 < len && s.charAt(index) == '0'
                    && (s.charAt(index + 1) == 'x' || s.charAt(index + 1) == 'X')) {
                r = 16;
                index += 2;
            }
            /* steps 13-16 */
            double number = StringToNumber.stringToNumber(s, index, r);
            /* step 17 */
            return (isPos ? number : -number);
        }

        /**
         * 15.7.2.10 Number.parseFloat (string)
         */
        @Function(name = "parseFloat", arity = 1)
        public static Object parseFloat(ExecutionContext cx, Object thisValue, Object string) {
            /* steps 1-2 */
            String inputString = ToFlatString(cx, string);
            /* step 3 */
            String trimmedString = Strings.trimLeft(inputString);
            /* step 4 */
            if (trimmedString.isEmpty()) {
                return Double.NaN;
            }
            /* steps 5-6 */
            return readDecimalLiteralPrefix(trimmedString, 0, trimmedString.length());
        }

        /**
         * 15.7.2.11 Number.isNaN (number)
         */
        @Function(name = "isNaN", arity = 1)
        public static Object isNaN(ExecutionContext cx, Object thisValue, Object number) {
            if (!Type.isNumber(number)) {
                return false;
            }
            return Double.isNaN(Type.numberValue(number));
        }

        /**
         * 15.7.2.12 Number.isFinite (number)
         */
        @Function(name = "isFinite", arity = 1)
        public static Object isFinite(ExecutionContext cx, Object thisValue, Object number) {
            if (!Type.isNumber(number)) {
                return false;
            }
            double num = Type.numberValue(number);
            return !(Double.isInfinite(num) || Double.isNaN(num));
        }

        /**
         * 15.7.2.13 Number.isInteger (number)
         */
        @Function(name = "isInteger", arity = 1)
        public static Object isInteger(ExecutionContext cx, Object thisValue, Object number) {
            if (!Type.isNumber(number)) {
                return false;
            }
            double num = Type.numberValue(number);
            if (Double.isNaN(num) || Double.isInfinite(num)) {
                return false;
            }
            double integer = ToInteger(num);
            return integer == num;
        }

        /**
         * 15.7.2.14 Number.isSafeInteger (number)
         */
        @Function(name = "isSafeInteger", arity = 1)
        public static Object isSafeInteger(ExecutionContext cx, Object thisValue, Object number) {
            if (!Type.isNumber(number)) {
                return false;
            }
            double num = Type.numberValue(number);
            if (Double.isNaN(num) || Double.isInfinite(num)) {
                return false;
            }
            double integer = ToInteger(num);
            if (integer != num) {
                return false;
            }
            return (Math.abs(integer) <= 0x1FFFFFFFFFFFFFL);
        }

        /**
         * 15.7.2.15 Number[ @@create ] ( )
         */
        @Function(name = "@@create", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.NumberPrototype,
                    NumberObjectAllocator.INSTANCE);
        }
    }

    private static class NumberObjectAllocator implements ObjectAllocator<NumberObject> {
        static final ObjectAllocator<NumberObject> INSTANCE = new NumberObjectAllocator();

        @Override
        public NumberObject newInstance(Realm realm) {
            return new NumberObject(realm);
        }
    }

    private static double readDecimalLiteralPrefix(String s, int start, int end) {
        final int Infinity_length = "Infinity".length();

        int index = start;
        int c = s.charAt(index++);
        boolean isPos = true;
        if (c == '+' || c == '-') {
            if (index >= end)
                return Double.NaN;
            isPos = (c == '+');
            c = s.charAt(index++);
        }
        if (c == 'I') {
            // Infinity
            if (index - 1 + Infinity_length <= end
                    && s.regionMatches(index - 1, "Infinity", 0, Infinity_length)) {
                return isPos ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            }
            return Double.NaN;
        }
        prefix: {
            if (c == '.') {
                if (index >= end)
                    return Double.NaN;
                char d = s.charAt(index);
                if (!(d >= '0' && d <= '9')) {
                    return Double.NaN;
                }
            } else {
                if (!(c >= '0' && c <= '9')) {
                    return Double.NaN;
                }
                do {
                    if (!(c >= '0' && c <= '9')) {
                        break prefix;
                    }
                    if (index >= end) {
                        break;
                    }
                    c = s.charAt(index++);
                } while (c != '.' && c != 'e' && c != 'E');
            }
            if (c == '.') {
                while (index < end) {
                    c = s.charAt(index++);
                    if (c == 'e' || c == 'E') {
                        break;
                    }
                    if (!(c >= '0' && c <= '9')) {
                        break prefix;
                    }
                }
            }
            if (c == 'e' || c == 'E') {
                if (index >= end)
                    break prefix;
                int exp = index;
                c = s.charAt(index++);
                if (c == '+' || c == '-') {
                    if (index >= end) {
                        index = exp;
                        break prefix;
                    }
                    c = s.charAt(index++);
                }
                if (!(c >= '0' && c <= '9')) {
                    index = exp;
                    break prefix;
                }
                do {
                    if (!(c >= '0' && c <= '9')) {
                        break prefix;
                    }
                    if (index >= end) {
                        break;
                    }
                    c = s.charAt(index++);
                } while (true);
            }
            if (index >= end) {
                return Double.parseDouble(s.substring(start, end));
            }
        } // prefix
        return Double.parseDouble(s.substring(start, index - 1));
    }
}
