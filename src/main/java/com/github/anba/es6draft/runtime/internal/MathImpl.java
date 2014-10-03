/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

/**
 * Java port of fdlibm functions.
 */
public final class MathImpl {
    private MathImpl() {
    }

    /* @formatter:off */
    /* @(#)e_log.c 1.3 95/01/18 */
    /*
     * ====================================================
     * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
     *
     * Developed at SunSoft, a Sun Microsystems, Inc. business.
     * Permission to use, copy, modify, and distribute this
     * software is freely granted, provided that this notice
     * is preserved.
     * ====================================================
     */

    private static final double
    log2e   =  1.442695040888963407359924,  /* 3FF71547 652B82FE */
    two54   =  1.80143985094819840000e+16,  /* 43500000 00000000 */
    Lg1 = 6.666666666666735130e-01,  /* 3FE55555 55555593 */
    Lg2 = 3.999999999940941908e-01,  /* 3FD99999 9997FA04 */
    Lg3 = 2.857142874366239149e-01,  /* 3FD24924 94229359 */
    Lg4 = 2.222219843214978396e-01,  /* 3FCC71C5 1D8E78AF */
    Lg5 = 1.818357216161805012e-01,  /* 3FC74664 96CB03DE */
    Lg6 = 1.531383769920937332e-01,  /* 3FC39A09 D078C69F */
    Lg7 = 1.479819860511658591e-01;  /* 3FC2F112 DF3E5244 */

    private static final double zero = 0.0;

    /**
     * Returns the base 2 logarithm of {@code x}.
     * <p>
     * This method computes {@code Math.log(x) / Math.log(2.0)}, but may yield better accuracy.
     * 
     * @param x
     *            a double value
     * @return the base 2 logarithm of {@code x}
     */
    public static double
    log2(double x)
    {
        double hfsq,f,s,z,R,w,t1,t2,dk;
        int k,hx,i,j;
        /* unsigned */ int lx;

        long bits = Double.doubleToRawLongBits(x);
        hx = __HI(bits);       /* high word of x */
        lx = __LO(bits);       /* low  word of x */

        k=0;
        if (hx < 0x00100000) {          /* x < 2**-1022  */
            if (((hx&0x7fffffff)|lx)==0)
            return -two54/zero;     /* log(+-0)=-inf */
            if (hx<0) return (x-x)/zero;    /* log(-#) = NaN */
            k -= 54; x *= two54; /* subnormal number, scale up x */
            hx = __HI(x);       /* high word of x */
        }
        if (hx >= 0x7ff00000) return x+x;
        k += (hx>>20)-1023;
        hx &= 0x000fffff;
        i = (hx+0x95f64)&0x100000;
        x = __HI(x, hx|(i^0x3ff00000));    /* normalize x or x/2 */
        k += (i>>20);
        f = x-1.0;
        if((0x000fffff&(2+hx))<3) { /* |f| < 2**-20 */
            if(f==zero) {
                if(k==0) return zero;
                dk=(double)k;
                return dk;
            }
            R = f*f*(0.5-0.33333333333333333*f);
            if(k==0) return (f - R) * log2e;
            dk=(double)k;
            return dk - (R - f) * log2e;
        }
        s = f/(2.0+f);
        dk = (double)k;
        z = s*s;
        i = hx-0x6147a;
        w = z*z;
        j = 0x6b851-hx;
        t1= w*(Lg2+w*(Lg4+w*Lg6));
        t2= z*(Lg1+w*(Lg3+w*(Lg5+w*Lg7)));
        i |= j;
        R = t2+t1;
        if(i>0) {
            hfsq=0.5*f*f;
            if(k==0) return (f - (hfsq - s * (hfsq + R))) * log2e;
            return dk - (hfsq - s * (hfsq + R) - f) * log2e;
        } else {
            if(k==0) return (f - s * (f - R)) * log2e;
            return dk - (s * (f - R) - f) * log2e;
        }
    }
    /* @formatter:on */

    private static int __HI(long bits) {
        return (int) (bits >>> 32);
    }

    private static int __LO(long bits) {
        return (int) bits;
    }

    private static int __HI(double value) {
        return __HI(Double.doubleToRawLongBits(value));
    }

    private static int __LO(double value) {
        return __LO(Double.doubleToRawLongBits(value));
    }

    private static double __HI(double value, int hi) {
        return Double.longBitsToDouble(((long) hi << 32) | ((long) __LO(value) & 0xFFFF_FFFFL));
    }
}
