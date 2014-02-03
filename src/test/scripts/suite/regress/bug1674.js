/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 15.13.6.3.6: out-of-bounds index calculated
// https://bugs.ecmascript.org/show_bug.cgi?id=1674

let b = new ArrayBuffer(2);
let ta = new Int8Array(b);
ta.set([1], 1);
assertSame(0, ta[0]);
assertSame(1, ta[1]);
