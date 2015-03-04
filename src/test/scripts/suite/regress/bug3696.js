/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, fail
} = Assert;

// 22.2.3.22.2%TypedArray%.prototype.set: Add constructor parameter to CloneArrayBuffer ?
// https://bugs.ecmascript.org/show_bug.cgi?id=3696

var b = new ArrayBuffer(2);
Object.defineProperty(b, "constructor", {
  get() {
    fail `.constructor getter called`;
  }
});

var ta = new Int8Array(b);
ta[0] = 1;
ta[1] = 2;

ta.set(new Int8Array(b, 1), 0);

assertSame(2, ta[0]);
assertSame(2, ta[1]);
