/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 22.2.3.25 %TypedArray%.prototype.sort: Sort -0 before +0 ?
// https://bugs.ecmascript.org/show_bug.cgi?id=4230

var ta = new Float64Array([+0, -0]);

assertSame(+0, ta[0]);
assertSame(-0, ta[1]);

ta.sort();

assertSame(-0, ta[0]);
assertSame(+0, ta[1]);
