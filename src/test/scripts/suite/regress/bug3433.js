/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// Are %TypedArray% objects supposed to be Concat-Spreadable?
// https://bugs.ecmascript.org/show_bug.cgi?id=3433

var ta1 = new Int8Array(10);
var ta2 = new Int8Array(20);

assertSame(2, [].concat(ta1, ta2).length);
