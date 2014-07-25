/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertTrue, assertThrows, fail
} = Assert;

// 22.2.3.22 %TypedArray%.prototype.set: Missing neutered array buffer checks
// https://bugs.ecmascript.org/show_bug.cgi?id=3045

// NB: Internal API calls to neuter array buffer needed

// First check on function entry
{
  let ta = new Int8Array(10);
  neuterArrayBuffer(ta.buffer);
  assertThrows(
    () => ta.set([], {valueOf(){ fail `ToInteger(offset) called on neutered buffer` }})
  , TypeError);
}

// Next check is in loop
{
  let ta = new Int8Array(10);
  assertThrows(
    () => ta.set([], {valueOf() { neuterArrayBuffer(ta.buffer); return -1; }})
  , RangeError);
}

// Next check is in loop
{
  let ta = new Int8Array(10);
  let array = {get length() { neuterArrayBuffer(ta.buffer); return 20; }};
  assertThrows(() => ta.set(array, 0), RangeError);
}

// Neuter buffer in loop on first element
{
  let ta = new Int8Array(10);
  let array = Object.defineProperty([], "0", {get(){ neuterArrayBuffer(ta.buffer); return 0 }});
  assertThrows(() => ta.set(array, 0), TypeError);
}

// Neuter buffer in loop on second element
{
  let ta = new Int8Array(10);
  let zeroElement = false;
  let array = Object.defineProperty([], "0", {get(){ zeroElement = true; return 0 }});
  array = Object.defineProperty(array, "1", {get(){ neuterArrayBuffer(ta.buffer); return 0 }});
  assertThrows(() => ta.set(array, 0), TypeError);
  assertTrue(zeroElement);
}
