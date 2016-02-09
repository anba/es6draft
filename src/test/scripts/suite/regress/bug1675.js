/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.13.6.3.7: full ArrayBuffer clone necessary or srcByteOffset needs to be adjusted
// https://bugs.ecmascript.org/show_bug.cgi?id=1675

let buf = new ArrayBuffer(8);
let ta = new Int8Array(buf);
ta.forEach((v, k, a) => a[k] = k);

let src = new Int8Array(buf, 0, 4);
let dst = new Int8Array(buf, 2, 4);
dst.set(src);

assertSame(0, ta[0]);
assertSame(1, ta[1]);
assertSame(0, ta[2]);
assertSame(1, ta[3]);
assertSame(2, ta[4]);
assertSame(3, ta[5]);
assertSame(6, ta[6]);
assertSame(7, ta[7]);
