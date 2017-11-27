/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// ArrayBuffer() & ArrayBuffer("some non-number") doesn't match implementations
// https://github.com/tc39/ecma262/pull/265

assertSame(0, new ArrayBuffer().byteLength);
assertSame(0, new ArrayBuffer(void 0).byteLength);

assertSame(0, new ArrayBuffer("not-a-number").byteLength);

assertSame(0, new ArrayBuffer(+0).byteLength);
assertSame(0, new ArrayBuffer(-0).byteLength);

assertSame(0, new ArrayBuffer(+0.5).byteLength);
assertSame(0, new ArrayBuffer(-0.5).byteLength);

assertSame(1, new ArrayBuffer(+1.5).byteLength);
assertThrows(RangeError, () => new ArrayBuffer(-1.5));

assertThrows(RangeError, () => new ArrayBuffer(-1));
