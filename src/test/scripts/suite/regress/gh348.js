/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined
} = Assert;

// Annex B.3.3 semantics may leak arbitrary values via the synthesized outer var assignment
// https://github.com/tc39/ecma262/pull/348

function test() {
  assertSame("undefined", typeof f);
  assertUndefined(f);
  {
    assertSame("function", typeof f);
    f = 0;
    assertSame(0, f);
    function f() {}
    assertSame(0, f);
  }
  assertSame(0, f);
}
test();
