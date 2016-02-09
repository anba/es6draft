/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 24.1.4.3 ArrayBuffer.prototype.slice: Restore steps
// https://bugs.ecmascript.org/show_bug.cgi?id=3457

// class BrokenBuffer extends ArrayBuffer {
//   constructor() {
//     /* empty */
//   }
//   get [Symbol.species]() {
//     return BrokenBuffer;
//   }
// };

// var a = new ArrayBuffer(10);
// a.constructor = BrokenBuffer;
// assertThrows(TypeError, () => a.slice(0));
// assertThrows(TypeError, () => new BrokenBuffer().slice(0));
