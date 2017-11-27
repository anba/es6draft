/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.number;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInt32;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumeric;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.number.NumberObject.NumberCreate;

import org.mozilla.javascript.StringToNumber;

import com.github.anba.es6draft.parser.NumberParser;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>20 Numbers and Dates</h1><br>
 * <h2>20.1 Number Objects</h2>
 * <ul>
 * <li>20.1.1 The Number Constructor
 * <li>20.1.2 Properties of the Number Constructor
 * </ul>
 */
public final class NumberConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new Number constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public NumberConstructor(Realm realm) {
        super(realm, "Number", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 20.1.1.1 Number ( [ value ] )
     */
    @Override
    public Double call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* steps 1-3 */
        if (args.length == 0) {
            return 0d;
        }

        // Extension: BigInt:
        Number prim = ToNumeric(calleeContext, args[0]);
        if (Type.isBigInt(prim)) {
            return Type.bigIntValue(prim).doubleValue();
        }

        return prim.doubleValue();
        /* steps 4-6 (not applicable) */
    }

    /**
     * 20.1.1.1 Number ( [ value ] )
     */
    @Override
    public NumberObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        /* steps 1-2 */
        double n;
        if (args.length == 0) {
            n = 0d;
        } else {
            // Extension: BigInt:
            Number prim = ToNumeric(calleeContext, args[0]);
            if (Type.isBigInt(prim)) {
                n = Type.bigIntValue(prim).doubleValue();
            } else {
                n = prim.doubleValue();
            }
        }
        /* step 3 (not applicable) */
        /* steps 4-6 */
        return NumberCreate(calleeContext, n,
                GetPrototypeFromConstructor(calleeContext, newTarget, Intrinsics.NumberPrototype));
    }

    /**
     * 20.1.2 Properties of the Number Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "Number";

        /**
         * 20.1.2.15 Number.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.NumberPrototype;

        /**
         * 20.1.2.7 Number.MAX_VALUE
         */
        @Value(name = "MAX_VALUE", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Double MAX_VALUE = Double.MAX_VALUE;

        /**
         * 20.1.2.11 Number.MIN_VALUE
         */
        @Value(name = "MIN_VALUE", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Double MIN_VALUE = Double.MIN_VALUE;

        /**
         * 20.1.2.8 Number.NaN
         */
        @Value(name = "NaN", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Double NaN = Double.NaN;

        /**
         * 20.1.2.9 Number.NEGATIVE_INFINITY
         */
        @Value(name = "NEGATIVE_INFINITY",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Double NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY;

        /**
         * 20.1.2.14 Number.POSITIVE_INFINITY
         */
        @Value(name = "POSITIVE_INFINITY",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Double POSITIVE_INFINITY = Double.POSITIVE_INFINITY;

        /**
         * 20.1.2.1 Number.EPSILON
         */
        @Value(name = "EPSILON", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Double EPSILON = Math.ulp(1.0);

        /**
         * 20.1.2.6 Number.MAX_SAFE_INTEGER
         */
        @Value(name = "MAX_SAFE_INTEGER",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Double MAX_SAFE_INTEGER = 0x1F_FFFF_FFFF_FFFFp0;

        /**
         * 20.1.2.10 Number.MIN_SAFE_INTEGER
         */
        @Value(name = "MIN_SAFE_INTEGER",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Double MIN_SAFE_INTEGER = -0x1F_FFFF_FFFF_FFFFp0;

        /**
         * 20.1.2.13 Number.parseInt (string, radix)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string
         * @param radix
         *            the radix value
         * @return the parsed integer
         */
        @Function(name = "parseInt", arity = 2)
        public static Object parseInt(ExecutionContext cx, Object thisValue, Object string, Object radix) {
            /* step 1 */
            String inputString = ToFlatString(cx, string);
            /* step 2 */
            String s = Strings.trimLeft(inputString);
            int len = s.length();
            int index = 0;
            /* steps 3-5 */
            boolean isPos = true;
            if (index < len && (s.charAt(index) == '+' || s.charAt(index) == '-')) {
                isPos = s.charAt(index) == '+';
                index += 1;
            }
            /* step 6 */
            int r = ToInt32(cx, radix);
            /* step 7 */
            boolean stripPrefix = true;
            if (r != 0) {
                /* step 8 */
                if (r < 2 || r > 36) {
                    return Double.NaN;
                }
                stripPrefix = r == 16;
            } else {
                /* step 9 */
                r = 10;
            }
            /* step 10 */
            if (stripPrefix && index + 1 < len && s.charAt(index) == '0'
                    && (s.charAt(index + 1) == 'x' || s.charAt(index + 1) == 'X')) {
                r = 16;
                index += 2;
            }
            /* steps 11-13 */
            double number = StringToNumber.stringToNumber(s, index, r);
            /* steps 14-16 */
            return isPos ? number : -number;
        }

        /**
         * 20.1.2.12 Number.parseFloat (string)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string
         * @return the parsed number
         */
        @Function(name = "parseFloat", arity = 1)
        public static Object parseFloat(ExecutionContext cx, Object thisValue, Object string) {
            /* step 1 */
            String inputString = ToFlatString(cx, string);
            /* step 2 */
            String trimmedString = Strings.trimLeft(inputString);
            /* step 3 */
            if (trimmedString.isEmpty()) {
                return Double.NaN;
            }
            /* steps 4-7 */
            return readDecimalLiteralPrefix(trimmedString);
        }

        /**
         * 20.1.2.4 Number.isNaN (number)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param number
         *            the number value
         * @return {@code true} if the number is the NaN value
         */
        @Function(name = "isNaN", arity = 1)
        public static Object isNaN(ExecutionContext cx, Object thisValue, Object number) {
            /* step 1 */
            if (!Type.isNumber(number)) {
                return false;
            }
            /* steps 2-3 */
            return Double.isNaN(Type.numberValue(number));
        }

        /**
         * 20.1.2.2 Number.isFinite (number)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param number
         *            the number value
         * @return {@code true} if the argument is a finite number
         */
        @Function(name = "isFinite", arity = 1)
        public static Object isFinite(ExecutionContext cx, Object thisValue, Object number) {
            /* step 1 */
            if (!Type.isNumber(number)) {
                return false;
            }
            double num = Type.numberValue(number);
            /* steps 2-3 */
            return !(Double.isInfinite(num) || Double.isNaN(num));
        }

        /**
         * 20.1.2.3 Number.isInteger (number)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param number
         *            the number value
         * @return {@code true} if the argument is an integer
         */
        @Function(name = "isInteger", arity = 1)
        public static Object isInteger(ExecutionContext cx, Object thisValue, Object number) {
            /* step 1 */
            if (!Type.isNumber(number)) {
                return false;
            }
            double num = Type.numberValue(number);
            /* step 2 */
            if (Double.isNaN(num) || Double.isInfinite(num)) {
                return false;
            }
            /* steps 3-5 */
            return Math.rint(num) == num;
        }

        /**
         * 20.1.2.5 Number.isSafeInteger (number)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param number
         *            the number value
         * @return {@code true} if the argument is a safe integer
         */
        @Function(name = "isSafeInteger", arity = 1)
        public static Object isSafeInteger(ExecutionContext cx, Object thisValue, Object number) {
            /* step 1 */
            if (!Type.isNumber(number)) {
                return false;
            }
            double num = Type.numberValue(number);
            /* step 2 */
            if (Double.isNaN(num) || Double.isInfinite(num)) {
                return false;
            }
            /* steps 3-4 */
            if (Math.rint(num) != num) {
                return false;
            }
            /* steps 5-6 */
            return Math.abs(num) <= 0x1F_FFFF_FFFF_FFFFp0;
        }
    }

    private static double readDecimalLiteralPrefix(String s) {
        final int Infinity_length = "Infinity".length();

        final int end = s.length();
        int index = 0;
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
            if (index - 1 + Infinity_length <= end && s.regionMatches(index - 1, "Infinity", 0, Infinity_length)) {
                return isPos ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            }
            return Double.NaN;
        }
        boolean hasDot = false, hasExp = false;
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
                hasDot = true;
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
                hasExp = true;
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
                if (!(hasDot || hasExp)) {
                    return NumberParser.parseInteger(s);
                }
                return NumberParser.parseDecimal(s);
            }
        } // prefix
        if (!(hasDot || hasExp)) {
            return NumberParser.parseInteger(s, index - 1);
        }
        return NumberParser.parseDecimal(s, index - 1);
    }
}
