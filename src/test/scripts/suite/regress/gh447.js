/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// CloneArrayBuffer copies the underlying ArrayBuffer until the end, which callers don't need
// https://github.com/tc39/ecma262/pull/447

var ta = new Int8Array(new ArrayBuffer(10), 0, 4);
assertSame(4, new Int8Array(ta).buffer.byteLength);
