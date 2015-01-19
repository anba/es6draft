/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// ArrayBuffer.prototype.byteLength should not throw when neutered
// https://bugs.ecmascript.org/show_bug.cgi?id=2385

var arr = new ArrayBuffer(10);
assertSame(10, arr.byteLength);

detachArrayBuffer(arr);

assertThrows(TypeError, () => arr.byteLength);
