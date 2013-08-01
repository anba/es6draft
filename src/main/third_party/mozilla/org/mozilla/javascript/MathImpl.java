/* -*- Mode: C++; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 * vim: set ts=8 sts=4 et sw=4 tw=99:
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * Java port of acosh, asinh and atanh from mozilla-central/js/src/jsmath.cpp
 */
public class MathImpl {
    private static final double LN2 = Math.log(2d);
    private static final double EPSILON = Math.ulp(1.0);
    private static final double SQUARE_ROOT_EPSILON = Math.sqrt(EPSILON);
    private static final double FOURTH_ROOT_EPSILON = Math.sqrt(SQUARE_ROOT_EPSILON);

    private MathImpl() {
    }

    private static double sqrt1pm1(double x) {
        return Math.sqrt(1 + x) - 1;
    }

    public static
    double acosh(double x)
    {
        if ((x - 1) >= SQUARE_ROOT_EPSILON) {
            if (x > 1 / SQUARE_ROOT_EPSILON) {
                /*
                 * http://functions.wolfram.com/ElementaryFunctions/ArcCosh/06/01/06/01/0001/
                 * approximation by laurent series in 1/x at 0+ order from -1 to 0
                 */
                return Math.log(x) + LN2;
            } else if (x < 1.5) {
                // This is just a rearrangement of the standard form below
                // devised to minimize loss of precision when x ~ 1:
                double y = x - 1;
                return Math.log1p(y + Math.sqrt(y * y + 2 * y));
            } else {
                // http://functions.wolfram.com/ElementaryFunctions/ArcCosh/02/
                return Math.log(x + Math.sqrt(x * x - 1));
            }
        } else {
            // see http://functions.wolfram.com/ElementaryFunctions/ArcCosh/06/01/04/01/0001/
            double y = x - 1;
            // approximation by taylor series in y at 0 up to order 2.
            // If x is less than 1, sqrt(2 * y) is NaN and the result is NaN.
            return Math.sqrt(2 * y) * (1 - y / 12 + 3 * y * y / 160);
        }
    }

    public static
    double asinh(double x)
    {
        if (x >= FOURTH_ROOT_EPSILON) {
            if (x > 1 / SQUARE_ROOT_EPSILON)
                // http://functions.wolfram.com/ElementaryFunctions/ArcSinh/06/01/06/01/0001/
                // approximation by laurent series in 1/x at 0+ order from -1 to 1
                return LN2 + Math.log(x) + 1 / (4 * x * x);
            else if (x < 0.5)
                return Math.log1p(x + sqrt1pm1(x * x));
            else
                return Math.log(x + Math.sqrt(x * x + 1));
        } else if (x <= -FOURTH_ROOT_EPSILON) {
            return -asinh(-x);
        } else {
            // http://functions.wolfram.com/ElementaryFunctions/ArcSinh/06/01/03/01/0001/
            // approximation by taylor series in x at 0 up to order 2
            double result = x;

            if (Math.abs(x) >= SQUARE_ROOT_EPSILON) {
                double x3 = x * x * x;
                // approximation by taylor series in x at 0 up to order 4
                result -= x3 / 6;
            }

            return result;
        }
    }

    public static
    double atanh(double x)
    {
        if (Math.abs(x) >= FOURTH_ROOT_EPSILON) {
            // http://functions.wolfram.com/ElementaryFunctions/ArcTanh/02/
            if (Math.abs(x) < 0.5)
                return (Math.log1p(x) - Math.log1p(-x)) / 2;

            return Math.log((1 + x) / (1 - x)) / 2;
        } else {
            // http://functions.wolfram.com/ElementaryFunctions/ArcTanh/06/01/03/01/
            // approximation by taylor series in x at 0 up to order 2
            double result = x;

            if (Math.abs(x) >= SQUARE_ROOT_EPSILON) {
                double x3 = x * x * x;
                result += x3 / 3;
            }

            return result;
        }
    }
}
