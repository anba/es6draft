/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined, assertSyntaxError
} = Assert;

// B.3.3  Block-Level Function Declarations Web Legacy Compatibility Semantics
{
  const e = "fallback";

  assertSyntaxError(`
  function f1() {
    try {
    } catch (e) {
      function e() { }
    }
  }
  `);

  function f2() {
    assertUndefined(e);
    try {
      /* no throw */
    } catch (e) {
      { function e() { return "e" } }
    }
    assertUndefined(e);
  }
  f2();

  function f3() {
    assertUndefined(e);
    try {
      throw null;
    } catch (e) {
      { function e() { return "e" } }
    }
    assertSame("e", e());
  }
  f3();
}
