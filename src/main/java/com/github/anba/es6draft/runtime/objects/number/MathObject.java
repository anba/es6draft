/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.number;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToUint32;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import org.mozilla.javascript.MathImpl;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>20 Numbers and Dates</h1><br>
 * <h2>20.2 The Math Object</h2>
 * <ul>
 * <li>20.2.1 Value Properties of the Math Object
 * <li>20.2.2 Function Properties of the Math Object
 * </ul>
 */
public final class MathObject extends OrdinaryObject implements Initializable {
    public MathObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        setPrototype(cx.getIntrinsic(Intrinsics.ObjectPrototype));

        createProperties(cx, this, ValueProperties.class);
        createProperties(cx, this, FunctionProperties.class);
    }

    /**
     * 20.2.1 Value Properties of the Math Object
     */
    public enum ValueProperties {
        ;

        /**
         * 20.2.1.1 Math.E
         */
        @Value(name = "E", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double E = Math.E;

        /**
         * 20.2.1.2 Math.LN10
         */
        @Value(name = "LN10", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double LN10 = Math.log(10d);

        /**
         * 20.2.1.4 Math.LN2
         */
        @Value(name = "LN2", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double LN2 = Math.log(2d);

        /**
         * 20.2.1.5 Math.LOG2E
         */
        @Value(name = "LOG2E", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double LOG2E = 1d / Math.log(2d);

        /**
         * 20.2.1.3 Math.LOG10E
         */
        @Value(name = "LOG10E", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double LOG10E = Math.log10(Math.E);

        /**
         * 20.2.1.6 Math.PI
         */
        @Value(name = "PI", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double PI = Math.PI;

        /**
         * 20.2.1.7 Math.SQRT1_2
         */
        @Value(name = "SQRT1_2", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double SQRT1_2 = Math.sqrt(.5d);

        /**
         * 20.2.1.8 Math.SQRT2
         */
        @Value(name = "SQRT2", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Double SQRT2 = Math.sqrt(2d);

        /**
         * 20.2.1.9 Math[ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Math";
    }

    /**
     * 20.2.2 Function Properties of the Math Object
     */
    public enum FunctionProperties {
        ;

        /**
         * 20.2.2.1 Math.abs (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the absolute number value
         */
        @Function(name = "abs", arity = 1)
        public static Object abs(ExecutionContext cx, Object thisValue, Object x) {
            return Math.abs(ToNumber(cx, x));
        }

        /**
         * 20.2.2.2 Math.acos (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the arc cosine of <var>x</var>
         */
        @Function(name = "acos", arity = 1)
        public static Object acos(ExecutionContext cx, Object thisValue, Object x) {
            return Math.acos(ToNumber(cx, x));
        }

        /**
         * 20.2.2.4 Math.asin (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the arc sine of <var>x</var>
         */
        @Function(name = "asin", arity = 1)
        public static Object asin(ExecutionContext cx, Object thisValue, Object x) {
            return Math.asin(ToNumber(cx, x));
        }

        /**
         * 20.2.2.6 Math.atan (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the arc tangent of <var>x</var>
         */
        @Function(name = "atan", arity = 1)
        public static Object atan(ExecutionContext cx, Object thisValue, Object x) {
            return Math.atan(ToNumber(cx, x));
        }

        /**
         * 20.2.2.7 Math.atan2 (y, x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param y
         *            the first argument number
         * @param x
         *            the second argument number
         * @return the arc tangent of <var>y</var> and <var>x</var>
         */
        @Function(name = "atan2", arity = 2)
        public static Object atan2(ExecutionContext cx, Object thisValue, Object y, Object x) {
            return Math.atan2(ToNumber(cx, y), ToNumber(cx, x));
        }

        /**
         * 20.2.2.10 Math.ceil (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the smallest integer number not less than <var>x</var>
         */
        @Function(name = "ceil", arity = 1)
        public static Object ceil(ExecutionContext cx, Object thisValue, Object x) {
            return Math.ceil(ToNumber(cx, x));
        }

        /**
         * 20.2.2.12 Math.cos (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the cosine of <var>x</var>
         */
        @Function(name = "cos", arity = 1)
        public static Object cos(ExecutionContext cx, Object thisValue, Object x) {
            return Math.cos(ToNumber(cx, x));
        }

        /**
         * 20.2.2.14 Math.exp (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the value <i>e</i><span><sup><var>x</var></sup></span>
         */
        @Function(name = "exp", arity = 1)
        public static Object exp(ExecutionContext cx, Object thisValue, Object x) {
            return Math.exp(ToNumber(cx, x));
        }

        /**
         * 20.2.2.16 Math.floor (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the largest integer number not greater than <var>x</var>
         */
        @Function(name = "floor", arity = 1)
        public static Object floor(ExecutionContext cx, Object thisValue, Object x) {
            return Math.floor(ToNumber(cx, x));
        }

        /**
         * 20.2.2.20 Math.log (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the natural logarithm of <var>x</var>
         */
        @Function(name = "log", arity = 1)
        public static Object log(ExecutionContext cx, Object thisValue, Object x) {
            return Math.log(ToNumber(cx, x));
        }

        /**
         * 20.2.2.24 Math.max ( value1, value2 [ , ...values ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param values
         *            the argument values
         * @return the maximum number
         */
        @Function(name = "max", arity = 2)
        public static Object max(ExecutionContext cx, Object thisValue, Object... values) {
            double max = Double.NEGATIVE_INFINITY;
            for (Object value : values) {
                double v = ToNumber(cx, value);
                // Do not call `Double.compare(v, max)` (parameter order!), to handle NaN properly
                // N.B. Double.compare() includes all necessary checks to be compliant to Abstract
                // Relational Comparison Algorithm (11.8.1)
                if (Double.compare(max, v) != 1) {
                    max = v;
                }
            }
            return max;
        }

        /**
         * 20.2.2.25 Math.min ( value1, value2 [ , ...values ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param values
         *            the argument values
         * @return the minimum number
         */
        @Function(name = "min", arity = 2)
        public static Object min(ExecutionContext cx, Object thisValue, Object... values) {
            double min = Double.POSITIVE_INFINITY;
            for (Object value : values) {
                double v = ToNumber(cx, value);
                // Do not call `Double.compare(-v, -min)` (parameter order!), to handle NaN properly
                // N.B. Double.compare() includes all necessary checks to be compliant to Abstract
                // Relational Comparison Algorithm (11.8.1)
                if (Double.compare(-min, -v) != 1) {
                    min = v;
                }
            }
            return min;
        }

        /**
         * 20.2.2.26 Math.pow (x, y)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the first argument number
         * @param y
         *            the second argument number
         * @return <var>x</var> raised to the power <var>y</var>
         */
        @Function(name = "pow", arity = 2)
        public static Object pow(ExecutionContext cx, Object thisValue, Object x, Object y) {
            return Math.pow(ToNumber(cx, x), ToNumber(cx, y));
        }

        /**
         * 20.2.2.27 Math.random ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return a random number in the interval [0, 1)
         */
        @Function(name = "random", arity = 0)
        public static Object random(ExecutionContext cx, Object thisValue) {
            return cx.getRealm().getRandom().nextDouble();
        }

        /**
         * 20.2.2.28 Math.round (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the nearest integer value to <var>x</var>
         */
        @Function(name = "round", arity = 1)
        public static Object round(ExecutionContext cx, Object thisValue, Object x) {
            double d = ToNumber(cx, x);
            if (d != d || d == 0 || Double.isInfinite(d)) {
                return d;
            }
            if (d < 0 && d >= -0.5) {
                return -0.0d;
            }
            int exp = Math.getExponent(d);
            if (exp >= 52) {
                return d;
            }
            return Math.floor(d + 0.5);
        }

        /**
         * 20.2.2.30 Math.sin (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the sine of <var>x</var>
         */
        @Function(name = "sin", arity = 1)
        public static Object sin(ExecutionContext cx, Object thisValue, Object x) {
            return Math.sin(ToNumber(cx, x));
        }

        /**
         * 20.2.2.32 Math.sqrt (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the square root of its argument
         */
        @Function(name = "sqrt", arity = 1)
        public static Object sqrt(ExecutionContext cx, Object thisValue, Object x) {
            return Math.sqrt(ToNumber(cx, x));
        }

        /**
         * 20.2.2.33 Math.tan (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the tangent of <var>x</var>
         */
        @Function(name = "tan", arity = 1)
        public static Object tan(ExecutionContext cx, Object thisValue, Object x) {
            return Math.tan(ToNumber(cx, x));
        }

        /**
         * 20.2.2.21 Math.log10 (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the decimal logarithm of <var>x</var>
         */
        @Function(name = "log10", arity = 1)
        public static Object log10(ExecutionContext cx, Object thisValue, Object x) {
            return Math.log10(ToNumber(cx, x));
        }

        /**
         * 20.2.2.23 Math.log2 (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the binary logarithm of <var>x</var>
         */
        @Function(name = "log2", arity = 1)
        public static Object log2(ExecutionContext cx, Object thisValue, Object x) {
            return Math.log(ToNumber(cx, x)) / Math.log(2d);
        }

        /**
         * 20.2.2.22 Math.log1p (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the natural logarithm of <var>x</var> + 1
         */
        @Function(name = "log1p", arity = 1)
        public static Object log1p(ExecutionContext cx, Object thisValue, Object x) {
            return Math.log1p(ToNumber(cx, x));
        }

        /**
         * 20.2.2.15 Math.expm1 (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the value <i>e</i><span><sup><var>x</var></sup></span>-1
         */
        @Function(name = "expm1", arity = 1)
        public static Object expm1(ExecutionContext cx, Object thisValue, Object x) {
            return Math.expm1(ToNumber(cx, x));
        }

        /**
         * 20.2.2.13 Math.cosh(x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the hyperbolic cosine of <var>x</var>
         */
        @Function(name = "cosh", arity = 1)
        public static Object cosh(ExecutionContext cx, Object thisValue, Object x) {
            return Math.cosh(ToNumber(cx, x));
        }

        /**
         * 20.2.2.31 Math.sinh(x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the hyperbolic sine of <var>x</var>
         */
        @Function(name = "sinh", arity = 1)
        public static Object sinh(ExecutionContext cx, Object thisValue, Object x) {
            return Math.sinh(ToNumber(cx, x));
        }

        /**
         * 20.2.2.34 Math.tanh(x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the hyperbolic tangent of <var>x</var>
         */
        @Function(name = "tanh", arity = 1)
        public static Object tanh(ExecutionContext cx, Object thisValue, Object x) {
            return Math.tanh(ToNumber(cx, x));
        }

        /**
         * 20.2.2.3 Math.acosh(x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the hyperbolic arc cosine of <var>x</var>
         */
        @Function(name = "acosh", arity = 1)
        public static Object acosh(ExecutionContext cx, Object thisValue, Object x) {
            double d = ToNumber(cx, x);
            if (Double.isNaN(d) || d < 1.0) {
                return Double.NaN;
            }
            if (d == 1) {
                return +0.0;
            }
            if (d == Double.POSITIVE_INFINITY) {
                return Double.POSITIVE_INFINITY;
            }
            // return Math.log(d + Math.sqrt(d * d - 1.0));
            return MathImpl.acosh(d);
        }

        /**
         * 20.2.2.5 Math.asinh(x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the hyperbolic arc sine of <var>x</var>
         */
        @Function(name = "asinh", arity = 1)
        public static Object asinh(ExecutionContext cx, Object thisValue, Object x) {
            double d = ToNumber(cx, x);
            if (Double.isNaN(d) || d == 0.0 || Double.isInfinite(d)) {
                return d;
            }
            // return Math.log(d + Math.sqrt(d * d + 1.0));
            return MathImpl.asinh(d);
        }

        /**
         * 20.2.2.8 Math.atanh(x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the hyperbolic arc tangent of <var>x</var>
         */
        @Function(name = "atanh", arity = 1)
        public static Object atanh(ExecutionContext cx, Object thisValue, Object x) {
            double d = ToNumber(cx, x);
            if (Double.isNaN(d) || d < -1.0 || d > 1.0) {
                return Double.NaN;
            }
            if (d == -1.0) {
                return Double.NEGATIVE_INFINITY;
            }
            if (d == +1.0) {
                return Double.POSITIVE_INFINITY;
            }
            if (d == 0.0) {
                return d;
            }
            // return (Math.log(1.0 + d) - Math.log(1.0 - d)) / 2.0;
            return MathImpl.atanh(d);
        }

        /**
         * 20.2.2.18 Math.hypot ( value1 , value2 [ , ...values ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param values
         *            the argument values
         * @return the square root of the sum of squares of its arguments
         */
        @Function(name = "hypot", arity = 2)
        public static Object hypot(ExecutionContext cx, Object thisValue, Object... values) {
            boolean hasInfinity = false, hasNaN = false;
            double max = 0;
            double[] numbers = new double[values.length];
            for (int i = 0, len = values.length; i < len; ++i) {
                double num = ToNumber(cx, values[i]);
                if (Double.isInfinite(num)) {
                    hasInfinity = true;
                } else if (Double.isNaN(num)) {
                    hasNaN = true;
                } else {
                    num = Math.abs(num);
                    max = Math.max(max, num);
                    numbers[i] = num;
                }
            }
            if (hasInfinity) {
                return Double.POSITIVE_INFINITY;
            } else if (hasNaN) {
                return Double.NaN;
            } else if (max == 0.0) {
                return +0.0;
            }
            if (values.length == 1) {
                return numbers[0]; // sqrt(value1^2)
            } else if (values.length == 2) {
                return Math.hypot(numbers[0], numbers[1]);
            } else {
                // Kahan summation with normalisation
                double result = 0.0, c = 0.0;
                for (int i = 0, len = numbers.length; i < len; ++i) {
                    double num = numbers[i] / max;
                    double y = num * num - c;
                    double t = result + y;
                    c = (t - result) - y;
                    result = t;
                }
                return Math.sqrt(result) * max;
            }
        }

        /**
         * 20.2.2.35 Math.trunc(x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the integral part of <var>x</var>
         */
        @Function(name = "trunc", arity = 1)
        public static Object trunc(ExecutionContext cx, Object thisValue, Object x) {
            double d = ToNumber(cx, x);
            return d < 0 ? Math.ceil(d) : Math.floor(d);
        }

        /**
         * 20.2.2.29 Math.sign(x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the sign of <var>x</var>
         */
        @Function(name = "sign", arity = 1)
        public static Object sign(ExecutionContext cx, Object thisValue, Object x) {
            return Math.signum(ToNumber(cx, x));
        }

        /**
         * 20.2.2.9 Math.cbrt(x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the cubic root of its argument
         */
        @Function(name = "cbrt", arity = 1)
        public static Object cbrt(ExecutionContext cx, Object thisValue, Object x) {
            return Math.cbrt(ToNumber(cx, x));
        }

        /**
         * 20.2.2.19 Math.imul(x, y)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the first argument number
         * @param y
         *            the second argument number
         * @return the integer multiplication of its arguments
         */
        @Function(name = "imul", arity = 2)
        public static Object imul(ExecutionContext cx, Object thisValue, Object x, Object y) {
            long a = ToUint32(cx, x);
            long b = ToUint32(cx, y);
            return (int) (a * b);
        }

        /**
         * 20.2.2.17 Math.fround (x)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the value <i>float32</i>(<var>x</var>)
         */
        @Function(name = "fround", arity = 1)
        public static Object fround(ExecutionContext cx, Object thisValue, Object x) {
            double d = ToNumber(cx, x);
            if (Double.isNaN(d)) {
                return Double.NaN;
            }
            if (d == 0 || Double.isInfinite(d)) {
                return d;
            }
            float x32 = (float) d;
            double x64 = (double) x32;
            return x64;
        }

        /**
         * 20.2.2.11 Math.clz32 ( x )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param x
         *            the argument number
         * @return the number of leading zeroes in the 32-bit integer representation of the number
         */
        @Function(name = "clz32", arity = 1)
        public static Object clz32(ExecutionContext cx, Object thisValue, Object x) {
            /* steps 1-2 */
            long n = ToUint32(cx, x);
            /* steps 3-4 */
            return Integer.numberOfLeadingZeros((int) n);
        }
    }
}
