/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertTrue
} = Assert;

// 13.5.1.1: ConstructorMethod always returns empty
// https://bugs.ecmascript.org/show_bug.cgi?id=1648

{
  let called = false;
  class C {
    constructor() {
      assertFalse(called);
      called = true;
    }
  }
  new C();
  assertTrue(called);
}
