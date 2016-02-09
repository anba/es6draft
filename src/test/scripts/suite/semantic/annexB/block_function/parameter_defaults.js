/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// B.3.3  Block-Level Function Declarations Web Legacy Compatibility Semantics
{
  function nonStrict() {
    var b = 0;
    function g(a = function() {
      { function b(){} }
      return b;
    }) {
      return a();
    }
    assertSame("function", typeof g());
  }
  nonStrict();

  function strict() {
    "use strict";
    var b = 0;
    function g(a = function() {
      { function b(){} }
      return b;
    }) {
      return a();
    }
    assertSame(0, g());
  }
  strict();
}
