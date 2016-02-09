/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 22.2.2.4, %TypedArray%[@@create](): TypedArrays no longer subclassable out-of-the-box
// https://bugs.ecmascript.org/show_bug.cgi?id=2023

class MyInt8Array extends Int8Array { }

let ta = new MyInt8Array(10);
assertSame(10, ta.length);
assertSame(10, ta.byteLength);
assertSame(10, ta.buffer.byteLength);
