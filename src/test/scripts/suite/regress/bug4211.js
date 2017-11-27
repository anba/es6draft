/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 9.4.4.4 [[Set]]: Internal mapped property not updated
// https://bugs.ecmascript.org/show_bug.cgi?id=4211

var v = (function(a) {
  arguments[0] = 2;
  Object.defineProperty(arguments, "0", {writable: false});
  return arguments[0];
})(1);

assertSame(2, v);
