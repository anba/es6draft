/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 24.1.4.3 ArrayBuffer.prototype.slice: Check new ArrayBuffer is not same instance
// https://bugs.ecmascript.org/show_bug.cgi?id=3046

let buf = new ArrayBuffer(10);
buf.constructor = function Constructor(len) {
  return buf;
};
buf.constructor[Symbol.species] = buf.constructor;
assertThrows(TypeError, () => buf.slice(0));
