/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 22.2.1.5: Handle negative zero byteOffset
// https://bugs.ecmascript.org/show_bug.cgi?id=4544

var ta = new Int8Array(new ArrayBuffer(4), +0, 4);
assertSame(+0, ta.byteOffset);

var ta = new Int8Array(new ArrayBuffer(4), -0, 4);
assertSame(+0, ta.byteOffset);
