/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue, assertEquals
} = Assert;

// "enumerate" traps are not sprung when [[Enumerate]] accesses prototype properties
// https://bugs.ecmascript.org/show_bug.cgi?id=3724

var enumerateTrapCalled = false;
var o = {
  __proto__: new Proxy({A: 0}, {
    enumerate(target) {
      assertFalse(enumerateTrapCalled);
      enumerateTrapCalled = true;
      return Reflect.enumerate(target);
    }
  }),
  a: 0,
};

assertFalse(enumerateTrapCalled);
assertEquals(["a", "A"], [...Reflect.enumerate(o)]);
assertTrue(enumerateTrapCalled);
