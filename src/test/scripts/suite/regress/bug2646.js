/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue
} = Assert;

// 9.4.4.3 [[Get]]: Pass receiver argument when calling ordinary [[Get]] in step 8.a
// https://bugs.ecmascript.org/show_bug.cgi?id=2646

let r = (function(y) {
  var args = arguments;
  return Object.create(Object.defineProperty(arguments, "x", {
    get() {
      return this !== args;
    }
  })).x;
})(0);
assertTrue(r);
