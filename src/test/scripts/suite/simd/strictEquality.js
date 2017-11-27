/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse
} = Assert;

// Test Strict Equality semantics for +0/-0.
{
  let positiveZero = SIMD.Float32x4(+0, 1, 1, 1);
  let negativeZero = SIMD.Float32x4(-0, 1, 1, 1);

  assertTrue(positiveZero === positiveZero);
  assertTrue(negativeZero === negativeZero);
  assertTrue(positiveZero === negativeZero);
  assertTrue(negativeZero === positiveZero);
  assertTrue(SIMD.Float32x4(+0, 1, 1, 1) === negativeZero);
  assertTrue(SIMD.Float32x4(-0, 1, 1, 1) === negativeZero);

  assertFalse(positiveZero !== positiveZero);
  assertFalse(negativeZero !== negativeZero);
  assertFalse(positiveZero !== negativeZero);
  assertFalse(negativeZero !== positiveZero);
  assertFalse(SIMD.Float32x4(+0, 1, 1, 1) !== negativeZero);
  assertFalse(SIMD.Float32x4(-0, 1, 1, 1) !== negativeZero);
}

// Test Strict Equality semantics for NaN.
{
  let nan = SIMD.Float32x4(NaN, 0, 0, 0);
  let otherNaN = SIMD.Float32x4.fromInt32x4Bits(SIMD.Int32x4(0x7fc00000 | 1, 0, 0, 0));

  assertFalse(nan === nan);
  assertFalse(otherNaN === otherNaN);
  assertFalse(nan === otherNaN);
  assertFalse(otherNaN === nan);
  assertFalse(SIMD.Float32x4(NaN, 0, 0, 0) === nan);
  assertFalse(nan === SIMD.Float32x4(NaN, 0, 0, 0));

  assertTrue(nan !== nan);
  assertTrue(otherNaN !== otherNaN);
  assertTrue(nan !== otherNaN);
  assertTrue(otherNaN !== nan);
  assertTrue(SIMD.Float32x4(NaN, 0, 0, 0) !== nan);
  assertTrue(nan !== SIMD.Float32x4(NaN, 0, 0, 0));
}
