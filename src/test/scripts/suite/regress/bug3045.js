/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse, assertThrows, fail
} = Assert;

// 22.2.3.22 %TypedArray%.prototype.set: Missing detached array buffer checks
// https://bugs.ecmascript.org/show_bug.cgi?id=3045

// NB: Internal API calls to detach array buffer needed

// First check on function entry
{
  let ta = new Int8Array(10);
  let valueOfCalled = false;
  detachArrayBuffer(ta.buffer);
  assertThrows(TypeError,
    () => ta.set([], {valueOf() {
      assertFalse(valueOfCalled);
      valueOfCalled = true;
      return 0;
    }})
  );
  assertTrue(valueOfCalled);
}

// First check before ToObject(array)
{
  let ta = new Int8Array(10);
  detachArrayBuffer(ta.buffer);
  assertThrows(TypeError,
    () => ta.set(null)
  );
}

// Next check is in loop
{
  let ta = new Int8Array(10);
  assertThrows(RangeError,
    () => ta.set([], {valueOf() { detachArrayBuffer(ta.buffer); return -1; }})
  );
}

// Next check is in loop
{
  let ta = new Int8Array(10);
  let array = {get length() { detachArrayBuffer(ta.buffer); return 20; }};
  assertThrows(RangeError, () => ta.set(array, 0));
}

// Neuter buffer in loop on first element
{
  let ta = new Int8Array(10);
  let array = Object.defineProperty([], "0", {get(){ detachArrayBuffer(ta.buffer); return 0 }});
  assertThrows(TypeError, () => ta.set(array, 0));
}

// Neuter buffer in loop on second element
{
  let ta = new Int8Array(10);
  let zeroElement = false;
  let array = Object.defineProperty([], "0", {get(){ zeroElement = true; return 0 }});
  array = Object.defineProperty(array, "1", {get(){ detachArrayBuffer(ta.buffer); return 0 }});
  assertThrows(TypeError, () => ta.set(array, 0));
  assertTrue(zeroElement);
}
