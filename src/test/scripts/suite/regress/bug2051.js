/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 24.1.4.3 ArrayBuffer.prototype.slice: Ensure newly created ArrayBuffer is big enough
// https://bugs.ecmascript.org/show_bug.cgi?id=2051

let buffer = new ArrayBuffer(10);
buffer.constructor = function(len) {
  return new ArrayBuffer(5);
};
buffer.constructor[Symbol.species] = buffer.constructor;
assertThrows(TypeError, () => buffer.slice());

buffer.constructor = function(len) {
  return new ArrayBuffer(15);
};
buffer.constructor[Symbol.species] = buffer.constructor;
assertSame(15, buffer.slice().byteLength);
