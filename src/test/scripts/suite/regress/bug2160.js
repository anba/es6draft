/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 22.2.1.1 %TypedArray%, 24.1.2.1 ArrayBuffer: Replace SameValue with SameValueZero?
// https://bugs.ecmascript.org/show_bug.cgi?id=2160

assertSame(+0, new ArrayBuffer(-0).byteLength);
assertSame(+0, new Int8Array(-0).byteLength);
assertSame(+0, new Int8Array(-0).length);
