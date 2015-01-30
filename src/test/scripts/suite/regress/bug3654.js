/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 22.2.1.1.1 AllocateTypedArray: Move step 1 to callers
// https://bugs.ecmascript.org/show_bug.cgi?id=3654

const TypedArray = Object.getPrototypeOf(Int8Array);

// 22.2.1.1 %TypedArray% ( length )
assertThrows(RangeError, () => TypedArray(0.5));

// 22.2.1.2 %TypedArray% ( typedArray )
var source = new Int8Array(0);
detachArrayBuffer(source.buffer);
assertThrows(TypeError, () => TypedArray(source));

// 22.2.1.3 %TypedArray% ( object )
class Err extends Error { }
assertThrows(Err, () => TypedArray({
  [Symbol.iterator]() {
    throw new Err;
  }
}));

// 22.2.1.4 %TypedArray% ( buffer [ , byteOffset [ , length ] ] )
var buffer = new ArrayBuffer(0);
assertThrows(TypeError, () => TypedArray(buffer, -1));
