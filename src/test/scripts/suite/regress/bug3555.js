/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 9.4.1.2 [[Construct]] for bound function: F → target
// https://bugs.ecmascript.org/show_bug.cgi?id=3555

function F() {
  return new.target;
}
var boundF = F.bind(null);
var o = new boundF();
assertSame(o, F);
