/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToUint32;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.intl.NumberFormatPrototype.FormatNumber;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import org.mozilla.javascript.DToA;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.intl.NumberFormatConstructor;
import com.github.anba.es6draft.runtime.objects.intl.NumberFormatObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.7 Number Objects</h2>
 * <ul>
 * <li>15.7.4 Properties of the Number Prototype Object
 * </ul>
 */
public class NumberPrototype extends OrdinaryObject implements Initialisable {
    public NumberPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 15.7.4 Properties of the Number Prototype Object
     */
    public enum Properties {
        ;

        /**
         * Abstract operation thisNumberValue(value)
         */
        private static double thisNumberValue(ExecutionContext cx, Object object) {
            if (Type.isNumber(object)) {
                return Type.numberValue(object);
            }
            if (object instanceof NumberObject) {
                NumberObject obj = (NumberObject) object;
                if (obj.isInitialised()) {
                    return obj.getNumberData();
                }
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.7.4.1 Number.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Number;

        /**
         * 15.7.4.2 Number.prototype.toString ( [ radix ] )
         */
        @Function(name = "toString", arity = 1)
        public static Object toString(ExecutionContext cx, Object thisValue, Object radix) {
            double r = 10;
            if (!Type.isUndefined(radix)) {
                r = ToInteger(cx, radix);
            }
            if (r < 2 || r > 36) {
                throw throwRangeError(cx, Messages.Key.InvalidRadix);
            }
            if (r == 10) {
                return ToString(thisNumberValue(cx, thisValue));
            }
            double val = thisNumberValue(cx, thisValue);

            // 9.1.8.1 ToString Applied to the Number Type
            // steps 1-4
            if (val != val) {
                return "NaN";
            } else if (val == Double.POSITIVE_INFINITY) {
                return "Infinity";
            } else if (val == Double.NEGATIVE_INFINITY) {
                return "-Infinity";
            } else if (val == 0.0) {
                return "0";
            }
            return DToA.JS_dtobasestr((int) r, val);
        }

        /**
         * 15.7.4.3 Number.prototype.toLocaleString()
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(ExecutionContext cx, Object thisValue, Object locales,
                Object options) {
            // N.B. permissible but no encouraged
            // ES5/6
            // return ToString(thisNumberValue(cx, thisValue));

            // ECMA-402
            double x = thisNumberValue(cx, thisValue);
            NumberFormatConstructor constructor = (NumberFormatConstructor) cx
                    .getIntrinsic(Intrinsics.Intl_NumberFormat);
            NumberFormatObject numberFormat = (NumberFormatObject) constructor.construct(cx,
                    locales, options);
            return FormatNumber(cx, numberFormat, x);
        }

        /**
         * 15.7.4.4 Number.prototype.valueOf ( )
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(ExecutionContext cx, Object thisValue) {
            return thisNumberValue(cx, thisValue);
        }

        /**
         * 15.7.4.5 Number.prototype.toFixed (fractionDigits)
         */
        @Function(name = "toFixed", arity = 1)
        public static Object toFixed(ExecutionContext cx, Object thisValue, Object fractionDigits) {
            double x = thisNumberValue(cx, thisValue);
            double f = ToInteger(cx, fractionDigits);
            if (f < 0 || f > 20) {
                throw throwRangeError(cx, Messages.Key.InvalidPrecision);
            }
            if (x != x) {
                return "NaN";
            }
            StringBuilder sb = new StringBuilder();
            DToA.JS_dtostr(sb, DToA.DTOSTR_FIXED, (int) f, x);
            return sb.toString();
        }

        /**
         * 15.7.4.6 Number.prototype.toExponential (fractionDigits)
         */
        @Function(name = "toExponential", arity = 1)
        public static Object toExponential(ExecutionContext cx, Object thisValue,
                Object fractionDigits) {
            double x = thisNumberValue(cx, thisValue);
            double f = ToInteger(cx, fractionDigits);
            if (x != x) {
                return "NaN";
            } else if (x == Double.POSITIVE_INFINITY) {
                return "Infinity";
            } else if (x == Double.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
            if (f < 0 || f > 20) {
                throw throwRangeError(cx, Messages.Key.InvalidPrecision);
            }
            StringBuilder sb = new StringBuilder();
            if (fractionDigits == UNDEFINED) {
                DToA.JS_dtostr(sb, DToA.DTOSTR_STANDARD_EXPONENTIAL, 1 + (int) f, x);
            } else {
                DToA.JS_dtostr(sb, DToA.DTOSTR_EXPONENTIAL, 1 + (int) f, x);
            }
            return sb.toString();
        }

        /**
         * 15.7.4.7 Number.prototype.toPrecision (precision)
         */
        @Function(name = "toPrecision", arity = 1)
        public static Object toPrecision(ExecutionContext cx, Object thisValue, Object precision) {
            double x = thisNumberValue(cx, thisValue);
            if (precision == UNDEFINED) {
                return ToString(x);
            }
            double p = ToInteger(cx, precision);
            if (x != x) {
                return "NaN";
            } else if (x == Double.POSITIVE_INFINITY) {
                return "Infinity";
            } else if (x == Double.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
            if (p < 1 || p > 21) {
                throw throwRangeError(cx, Messages.Key.InvalidPrecision);
            }
            StringBuilder sb = new StringBuilder();
            DToA.JS_dtostr(sb, DToA.DTOSTR_PRECISION, (int) p, x);
            return sb.toString();
        }

        /**
         * 15.7.4.8 Number.prototype.clz ()
         */
        @Function(name = "clz", arity = 0)
        public static Object clz(ExecutionContext cx, Object thisValue) {
            double x = thisNumberValue(cx, thisValue);
            long n = ToUint32(cx, x);
            return Integer.numberOfLeadingZeros((int) n);
        }
    }
}
