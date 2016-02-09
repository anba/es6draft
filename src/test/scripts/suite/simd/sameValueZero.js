/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertTrue, assertFalse
} = Assert;

// Test SameValueZero semantics for +0/-0.
let positiveZero = SIMD.Float32x4(+0, 1, 1, 1);
let negativeZero = SIMD.Float32x4(-0, 1, 1, 1);

// Check assertSame and assertNotSame work as expected.
assertSame(SIMD.Float32x4(+0, 1, 1, 1), positiveZero);
assertSame(SIMD.Float32x4(-0, 1, 1, 1), negativeZero);
assertNotSame(SIMD.Float32x4(+0, 1, 1, 1), negativeZero);
assertNotSame(SIMD.Float32x4(-0, 1, 1, 1), positiveZero);

// Check Strict Equality semantics.
assertTrue(SIMD.Float32x4(+0, 1, 1, 1) === negativeZero);
assertTrue(SIMD.Float32x4(-0, 1, 1, 1) === negativeZero);
assertTrue(positiveZero === negativeZero);

// Check Strict Equality and SameValue semantics.
assertTrue(Object.is(SIMD.Float32x4(+0, 1, 1, 1), positiveZero));
assertTrue(Object.is(SIMD.Float32x4(-0, 1, 1, 1), negativeZero));
assertFalse(Object.is(SIMD.Float32x4(+0, 1, 1, 1), negativeZero));
assertFalse(Object.is(SIMD.Float32x4(-0, 1, 1, 1), positiveZero));

// Map with positiveZero key.
{
  let map = new Map();

  map.set(positiveZero, "ok");
  assertSame(1, map.size);
  assertTrue(map.has(positiveZero));
  assertTrue(map.has(negativeZero));
  assertSame(SIMD.Float32x4(+0, 1, 1, 1), map.keys().next().value);
}

// Map with negativeZero key.
{
  let map = new Map();

  map.set(negativeZero, "ok");
  assertSame(1, map.size);
  assertTrue(map.has(positiveZero));
  assertTrue(map.has(negativeZero));
  assertSame(SIMD.Float32x4(-0, 1, 1, 1), map.keys().next().value);
}

// Set with positiveZero key.
{
  let set = new Set();

  set.add(positiveZero);
  assertSame(1, set.size);
  assertTrue(set.has(positiveZero));
  assertTrue(set.has(negativeZero));
  assertSame(SIMD.Float32x4(+0, 1, 1, 1), set.keys().next().value);
}

// Set with negativeZero key.
{
  let set = new Set();

  set.add(negativeZero);
  assertSame(1, set.size);
  assertTrue(set.has(positiveZero));
  assertTrue(set.has(negativeZero));
  assertSame(SIMD.Float32x4(-0, 1, 1, 1), set.keys().next().value);
}

// Array.prototype.includes
{
  assertTrue([positiveZero].includes(positiveZero));
  assertTrue([negativeZero].includes(negativeZero));
  assertTrue([positiveZero].includes(negativeZero));
  assertTrue([negativeZero].includes(positiveZero));

  assertTrue([positiveZero].includes(SIMD.Float32x4(+0, 1, 1, 1)));
  assertTrue([negativeZero].includes(SIMD.Float32x4(-0, 1, 1, 1)));
  assertTrue([positiveZero].includes(SIMD.Float32x4(-0, 1, 1, 1)));
  assertTrue([negativeZero].includes(SIMD.Float32x4(+0, 1, 1, 1)));
}
