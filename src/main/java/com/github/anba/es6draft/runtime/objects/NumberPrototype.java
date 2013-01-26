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
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import org.mozilla.javascript.DToA;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.7 Number Objects</h2>
 * <ul>
 * <li>15.7.4 Properties of the Number Prototype Object
 * </ul>
 */
public class NumberPrototype extends NumberObject implements Scriptable, Initialisable {
    public NumberPrototype(Realm realm) {
        super(realm, +0.0);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    /**
     * 15.7.4 Properties of the Number Prototype Object
     */
    public enum Properties {
        ;

        private static double numberValue(Realm realm, Object object) {
            if (Type.isNumber(object)) {
                return Type.numberValue(object);
            }
            if (object instanceof NumberObject) {
                return ((NumberObject) object).getNumberData();
            }
            throw throwTypeError(realm, "incompatible object");
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
        public static Object toString(Realm realm, Object thisValue, Object radix) {
            double r = 10;
            if (!Type.isUndefined(radix)) {
                r = ToInteger(realm, radix);
            }
            if (r < 2 || r > 36) {
                throw throwRangeError(realm, "invalid radix");
            }
            if (r == 10) {
                return ToString(numberValue(realm, thisValue));
            }
            double val = numberValue(realm, thisValue);

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
        public static Object toLocaleString(Realm realm, Object thisValue) {
            // N.B. permissible but no encouraged
            return ToString(numberValue(realm, thisValue));
        }

        /**
         * 15.7.4.4 Number.prototype.valueOf ( )
         */
        @Function(name = "valueOf", arity = 0)
        public static Object valueOf(Realm realm, Object thisValue) {
            return numberValue(realm, thisValue);
        }

        /**
         * 15.7.4.5 Number.prototype.toFixed (fractionDigits)
         */
        @Function(name = "toFixed", arity = 1)
        public static Object toFixed(Realm realm, Object thisValue, Object fractionDigits) {
            double f = ToInteger(realm, fractionDigits);
            if (f < 0 || f > 20) {
                throw throwRangeError(realm, "");
            }
            double x = numberValue(realm, thisValue);
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
        public static Object toExponential(Realm realm, Object thisValue, Object fractionDigits) {
            double x = numberValue(realm, thisValue);
            double f = ToInteger(realm, fractionDigits);
            if (x != x) {
                return "NaN";
            } else if (x == Double.POSITIVE_INFINITY) {
                return "Infinity";
            } else if (x == Double.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
            if (f < 0 || f > 20) {
                throw throwRangeError(realm, "");
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
        public static Object toPrecision(Realm realm, Object thisValue, Object precision) {
            double x = numberValue(realm, thisValue);
            if (precision == UNDEFINED) {
                return ToString(x);
            }
            double p = ToInteger(realm, precision);
            if (x != x) {
                return "NaN";
            } else if (x == Double.POSITIVE_INFINITY) {
                return "Infinity";
            } else if (x == Double.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
            if (p < 1 || p > 21) {
                throw throwRangeError(realm, "");
            }
            StringBuilder sb = new StringBuilder();
            DToA.JS_dtostr(sb, DToA.DTOSTR_PRECISION, (int) p, x);
            return sb.toString();
        }

        /**
         * 15.7.4.8 Number.prototype.clz ()
         */
        @Function(name = "clz", arity = 0)
        public static Object clz(Realm realm, Object thisValue) {
            double x = numberValue(realm, thisValue);
            long n = ToUint32(realm, x);
            return Integer.numberOfLeadingZeros((int) n);
        }
    }
}
