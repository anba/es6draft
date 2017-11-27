/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Create new ArrayBuffer with offset
{
  let ab = new Int8Array(10).map((v, i) => i).buffer;
  let ta = new Int8Array(ab, 2, 6);
  ta.constructor = {
    [Symbol.species]: function(len) {
      return new Int8Array(new ArrayBuffer(len + 4), 4);
    }
  };
  let tb = ta.slice(0);

  assertSame("2,3,4,5,6,7", ta.toString());
  assertSame("2,3,4,5,6,7", tb.toString());
}

// Source and target arrays use the same ArrayBuffer
{
  let ab = new Int8Array(10).map((v, i) => i).buffer;
  let ta = new Int8Array(ab, 2, 6);
  ta.constructor = {
    [Symbol.species]: function(len) {
      return new Int8Array(ab, 0, len);
    }
  };
  let tb = ta.slice(0);

  assertSame("4,5,6,7,6,7", ta.toString());
  assertSame("2,3,4,5,6,7", tb.toString());
  assertSame("2,3,4,5,6,7,6,7,8,9", new Int8Array(ab).toString());
}
