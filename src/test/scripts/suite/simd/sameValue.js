/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertTrue, assertFalse
} = Assert;

// Test SameValue semantics for +0/-0.
{
  let positiveZero = SIMD.Float32x4(+0, 1, 1, 1);
  let negativeZero = SIMD.Float32x4(-0, 1, 1, 1);

  // Check SameValue semantics.
  assertTrue(Object.is(SIMD.Float32x4(+0, 1, 1, 1), positiveZero));
  assertTrue(Object.is(SIMD.Float32x4(-0, 1, 1, 1), negativeZero));
  assertFalse(Object.is(SIMD.Float32x4(+0, 1, 1, 1), negativeZero));
  assertFalse(Object.is(SIMD.Float32x4(-0, 1, 1, 1), positiveZero));

  // Check assertSame and assertNotSame work expected.
  assertSame(SIMD.Float32x4(+0, 1, 1, 1), positiveZero);
  assertSame(SIMD.Float32x4(-0, 1, 1, 1), negativeZero);
  assertNotSame(SIMD.Float32x4(+0, 1, 1, 1), negativeZero);
  assertNotSame(SIMD.Float32x4(-0, 1, 1, 1), positiveZero);
}

// Test SameValue semantics for NaN.
{
  let nan = SIMD.Float32x4(NaN, 0, 0, 0);
  let otherNaN = SIMD.Float32x4.fromInt32x4Bits(SIMD.Int32x4(0x7fc00000 | 1, 0, 0, 0));

  assertTrue(Object.is(nan, nan));
  assertTrue(Object.is(otherNaN, otherNaN));
  assertTrue(Object.is(nan, otherNaN));
  assertTrue(Object.is(otherNaN, nan));
  assertTrue(Object.is(SIMD.Float32x4(NaN, 0, 0, 0), nan));
  assertTrue(Object.is(nan, SIMD.Float32x4(NaN, 0, 0, 0)));
}
