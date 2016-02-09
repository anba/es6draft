/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Test ordinary [[Set]] with NaN.
{
  let nanBits = 0x7fc00000;
  let nan = SIMD.Float32x4.fromInt32x4Bits(SIMD.Int32x4(nanBits, 0, 0, 0));
  let otherNaN = SIMD.Float32x4.fromInt32x4Bits(SIMD.Int32x4(nanBits | 1, 0, 0, 0));

  assertSame(nan, otherNaN);

  let obj = {};

  obj.prop = nan;
  assertSame(SIMD.Int32x4(nanBits, 0, 0, 0), SIMD.Int32x4.fromFloat32x4Bits(obj.prop));

  obj.prop = otherNaN;
  assertSame(SIMD.Int32x4(nanBits, 0, 0, 0), SIMD.Int32x4.fromFloat32x4Bits(obj.prop));
}
