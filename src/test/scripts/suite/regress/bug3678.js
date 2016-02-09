/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 24.1.1.4 CloneArrayBuffer: Add IsDetachedBuffer check after steps 10-11
// https://bugs.ecmascript.org/show_bug.cgi?id=3678

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
assertThrows(TypeError, () => new Int8Array(ta));
