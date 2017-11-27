/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 22.2.1.5 %TypedArray%: Change "all other argument combinations" to "no arguments" or remove ?
// https://bugs.ecmascript.org/show_bug.cgi?id=4228

assertSame(0, new Int8Array().length);
assertSame(0, new Int8Array(void 0).length);
assertSame(0, new Int8Array(null).length);
