/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// Classes with computed names can see incomplete object
// https://bugs.ecmascript.org/show_bug.cgi?id=3302

var log = "";
assertThrows(ReferenceError, () => {
  class C {
    [(log += "a", 0)]() {}
    [(log += "b", C.prototype, log += "c", 1)]() {}
    [(log += "d", 0)]() {}
  }
});
assertSame("ab", log);
