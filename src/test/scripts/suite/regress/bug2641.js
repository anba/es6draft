/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 9.4.4.4 [[Set]]: Exotic arguments [[Set]] does not check receiver
// https://bugs.ecmascript.org/show_bug.cgi?id=2641

let r = (function(y) {
  Object.create(arguments)[0] = 2;
  return y;
})(1);
assertSame(1, r);
