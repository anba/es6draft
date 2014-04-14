/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertThrows
} = Assert;

// 15.13.6.1: %TypedArray% constructors and constructor selection issues
// https://bugs.ecmascript.org/show_bug.cgi?id=1672

// Ignore extra arguments, convert non-object types with ToNumber
assertSame(10, new Int8Array(10, 0).length);
assertSame(10, new Int8Array("10").length);

// Throw error when attempting to initialize with uninitialized object
assertThrows(() => new Int8Array(Int8Array[Symbol.create]()), TypeError);

// Copied typed array starts from byteOffset, copy contains same content
{
  let byteLength = 4, byteOffset = 2;
  let buf = new ArrayBuffer(byteLength);
  new Int8Array(buf).forEach((v, k, a) => a[k] = k);

  let ta = new Int8Array(buf, byteOffset);
  assertSame(byteOffset, ta.byteOffset);
  assertSame(byteLength - byteOffset, ta.byteLength);

  // Create typed array copy
  let copy = new Int8Array(ta);
  assertSame(0, copy.byteOffset);
  assertSame(byteLength - byteOffset, copy.byteLength);

  // Inspect and compare content
  let visited = 0;
  new Int8Array(buf).forEach((v, k, a) => {
    assertSame(k, a[k]);
    if (k >= byteOffset) {
      visited += 1;
      assertSame(k, ta[k - byteOffset]);
      assertSame(k, copy[k - byteOffset]);
    }
  });
  assertSame(byteLength - byteOffset, visited);
}

// ToLength() for array-like input
assertSame(0, new Int8Array({length: 0.6}).length);
assertSame(1, new Int8Array({length: 1.6}).length);
assertSame(0, new Int8Array({length: -1}).length);

// Copies content from array-like
assertSame(4, new Int8Array({length: 1, 0: 4})[0]);

// Throw error when attempting to initialize with uninitialized object
assertThrows(() => new Int8Array(ArrayBuffer[Symbol.create]()), TypeError);

// Throw RangeError when (byteLength - byteOffset) < 0
assertSame(1, new Int32Array(new ArrayBuffer(4), 0).length);
assertSame(0, new Int32Array(new ArrayBuffer(4), 4).length);
assertThrows(() => new Int32Array(new ArrayBuffer(4), 8), RangeError);
