/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError
} = Assert;

// LineTerminator between 'yield' and '*' not allowed
{
  assertSyntaxError(`
    function* g() {
      yield
      *
      a;
    }
  `);
  
  assertSyntaxError(`
    function* g() {
      yield
      * a;
    }
  `);
}

// LineTerminator between 'yield*' and <expression> allowed
{
  function g1() {
    yield *
    a;
  }
}
