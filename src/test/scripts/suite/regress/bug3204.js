/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame
} = Assert;

// B.3.3 Block-Level Function Declarations: Missing variable binding when parameter with same name as function is present
// https://bugs.ecmascript.org/show_bug.cgi?id=3204

{
  function g(f) {
    let r;
    { function f(){} r = f; }
    assertNotSame(r, f);
  }
  g();
  g(1);
}

{
  function g(f = 0) {
    let r;
    { function f(){} r = f; }
    assertNotSame(r, f);
  }
  g();
  g(1);
}
