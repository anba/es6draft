/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertSyntaxError
} = Assert;

// Normative: Allow duplicate FunctionDeclarations in a block
// https://github.com/tc39/ecma262/pull/453

{
  function globalBlockFn() { return 1; }
  function globalBlockFn() { return 2; }
  assertSame(2, globalBlockFn());
}
// assertSame(2, globalBlockFn());

(function() {
  {
    function functionBlockFn() { return 1; }
    function functionBlockFn() { return 2; }
    assertSame(2, functionBlockFn());
  }
  // assertSame(2, functionBlockFn());
})();

assertSyntaxError(`
"use strict";
{
  function strictBlockFn() { return 1; }
  function strictBlockFn() { return 2; }
}
`);
