/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertEquals
} = Assert;

const isLittleEndian = new Uint8Array(new Uint16Array([1]).buffer)[0] !== 0;

function geti64(i32, i) {
  return [i32[i * 2 + isLittleEndian], i32[i * 2 + !isLittleEndian]];
}

function seti64(i32, i, [hi, lo]) {
  i32[i * 2 + isLittleEndian] = hi;
  i32[i * 2 + !isLittleEndian] = lo;
}

const NaNs = {
  Float32: [
    0x7F800001|0, // smallest SNaN
    0x7FBFFFFF|0, // largest SNaN
    0x7FC00000|0, // smallest QNaN
    0x7FFFFFFF|0, // largest QNaN
    0xFF800001|0, // smallest SNaN, sign-bit set
    0xFFBFFFFF|0, // largest SNaN, sign-bit set
    0xFFC00000|0, // smallest QNaN, sign-bit set
    0xFFFFFFFF|0, // largest QNaN, sign-bit set
  ],
  Float64: [
    [0x7FF00000|0, 0x00000001|0], // smallest SNaN
    [0x7FF7FFFF|0, 0xFFFFFFFF|0], // largest SNaN
    [0x7FF80000|0, 0x00000000|0], // smallest QNaN
    [0x7FFFFFFF|0, 0xFFFFFFFF|0], // largest QNaN
    [0xFFF00000|0, 0x00000001|0], // smallest SNaN, sign-bit set
    [0xFFF7FFFF|0, 0xFFFFFFFF|0], // largest SNaN, sign-bit set
    [0xFFF80000|0, 0x00000000|0], // smallest QNaN, sign-bit set
    [0xFFFFFFFF|0, 0xFFFFFFFF|0], // largest QNaN, sign-bit set
  ],
};

const cNaN = {
  Float32: new Int32Array(new Float32Array([NaN]).buffer)[0],
  Float64: geti64(new Int32Array(new Float64Array([NaN]).buffer), 0),
};

function comparator(a, b) {
  if (a !== a) {
    return b !== b ? 0 : 1;
  }
  if (b !== b) {
    return -1;
  }
  if (a === b) {
    if (a === 0) {
      if (1 / a < 0) {
        return 1 / b < 0 ? 0 : -1;
      }
      return 1 / b < 0 ? 1 : 0;
    }
    return 0;
  }
  return a < b ? -1 : 1;
}

// Float32
for (let cmp of [void 0, comparator]) {
  const len = NaNs.Float32.length;
  let f32 = new Float32Array(len);
  let i32 = new Int32Array(f32.buffer);

  for (let i = 0; i < len; ++i) {
   i32[i] = NaNs.Float32[i];
  }

  f32.sort(cmp);

  // NaN bits canonicalized.
  for (let i = 0; i < len; ++i) {
   assertEquals(cNaN.Float32, i32[i]);
  }
}

// Float32: single element typed arrays
for (let cmp of [void 0, comparator]) {
  for (let i = 0; i < NaNs.Float32.length; ++i) {
    let f32 = new Float32Array(1);
    let i32 = new Int32Array(f32.buffer);

    i32[0] = NaNs.Float32[i];

    f32.sort(cmp);

    // Same bits.
    assertSame(NaNs.Float32[i], i32[0]);
  }
}

// Float64
for (let cmp of [void 0, comparator]) {
  const len = NaNs.Float64.length;
  let f64 = new Float64Array(len);
  let i32 = new Int32Array(f64.buffer);

  for (let i = 0; i < len; ++i) {
    seti64(i32, i, NaNs.Float64[i]);
  }

  f64.sort(cmp);

  // NaN bits canonicalized.
  for (let i = 0; i < len; ++i) {
    assertEquals(cNaN.Float64, geti64(i32, i));
  }
}

// Float64: single element typed arrays
for (let cmp of [void 0, comparator]) {
  for (let i = 0; i < NaNs.Float64.length; ++i) {
    let f64 = new Float64Array(1);
    let i32 = new Int32Array(f64.buffer);

    seti64(i32, 0, NaNs.Float64[i]);

    f64.sort(cmp);

    // Same bits.
    assertEquals(NaNs.Float64[i], geti64(i32, 0));
  }
}
