/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 22.2.3.23 %TypedArray%.prototype.slice: Various issues
// https://bugs.ecmascript.org/show_bug.cgi?id=4051

var ta = new Int8Array(10);
var r = ta.slice({valueOf() {
  detachArrayBuffer(ta.buffer);
  return 5;
}}, 5);
assertSame(0, r.length);
assertSame(Int8Array, r.constructor);

var ta = new Int8Array(10);
assertThrows(TypeError, () => ta.slice({valueOf() {
  detachArrayBuffer(ta.buffer);
  return 5;
}}, 6));

var ta = new Int8Array(10);
ta.constructor = Int16Array;
var r = ta.slice({valueOf() {
  detachArrayBuffer(ta.buffer);
  return 5;
}}, 5);
assertSame(0, r.length);
assertSame(Int16Array, r.constructor);

var ta = new Int8Array(10);
ta.constructor = Int16Array;
assertThrows(TypeError, () => ta.slice({valueOf() {
  detachArrayBuffer(ta.buffer);
  return 5;
}}, 6));
