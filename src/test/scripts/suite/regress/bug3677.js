/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 22.2.1.2 %TypedArray%: Swap steps 17.c and 17.d
// https://bugs.ecmascript.org/show_bug.cgi?id=3677

class Buffer extends ArrayBuffer {
  static get [Symbol.species]() {
    return Object.defineProperty(function() { }.bind(), "prototype", {
      get() {
        detachArrayBuffer(buf);
        return ArrayBuffer.prototype;
      }
    });
  }
}

let buf = new Buffer(10);
let ta = new Int8Array(buf);
assertThrows(TypeError, () => new Int16Array(ta));
