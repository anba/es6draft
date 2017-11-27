/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

{
  let sab = new SharedArrayBuffer(20);
  new Int8Array(sab).set(Array(20).fill(0).map((v, i) => i));
  setSharedArrayBuffer(sab);

  let ta = new Int8Array(getSharedArrayBuffer(), 0, 10);
  ta.constructor = {
    [Symbol.species]: function(len) {
      return new Int8Array(getSharedArrayBuffer(), 1, len);
    }
  };
  let tb = ta.slice(0);

  assertSame("0,0,0,0,0,0,0,0,0,0", ta.toString());
  assertSame("0,0,0,0,0,0,0,0,0,0", tb.toString());
}
