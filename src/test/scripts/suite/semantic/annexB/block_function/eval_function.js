/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows, assertUndefined
} = Assert;

// B.3.3  Block-Level Function Declarations Web Legacy Compatibility Semantics
// ES2016: EvalDeclarationInstantiation in functions

// Basic test: Check block scoped works in non-strict scripts.
(function() {
  assertSame("undefined", typeof f);
  eval(`{ function f() {} }`);
  assertSame("function", typeof f);
})();

// Basic test: Check block scoped not applied in strict scripts.
(function() {
  eval(`"use strict"; { function f() {} }`);
  assertSame("undefined", typeof f);
})();

// Legacy semantics not used if lexical found. (1)
(function() {
  let f = 0;
  eval(`
    assertSame("number", typeof f);
    { function f() {} }
    assertSame("number", typeof f);
  `);
  assertSame("number", typeof f);
})();

// Legacy semantics not used if lexical found. (2)
(function() {
  {
    let f = 0;
    eval(`
      assertSame("number", typeof f);
      { function f() {} }
      assertSame("number", typeof f);
    `);
    assertSame("number", typeof f);
  }
})();

// Function binding is initialized with "undefined".
(function() {
  eval(`
    assertUndefined(f);
    {
      assertSame("function", typeof f);
      function f(){}
      assertSame("function", typeof f);
    }
    assertSame("function", typeof f);
  `);
  assertSame("function", typeof f);
})();

// Legacy semantics used in nested eval calls.
(function() {
  eval(`
    assertSame("undefined", typeof f);
    eval(\`
      assertUndefined(f);
      { function f() {} }
      assertSame("function", typeof f);
    \`);
    assertSame("function", typeof f);
  `);
  assertSame("function", typeof f);
})();

// Legacy semantics in formal parameters environment.
(function(a = (eval(`{ function f() {} }`), () => f), b = () => f) {
  assertSame("function", typeof a());
  assertSame("undefined", typeof f);
  assertThrows(ReferenceError, () => f);
  assertThrows(ReferenceError, () => b());
})();

// Legacy semantics used if var-binding exists.
(function() {
  var f = 0;
  eval(`
    assertSame("number", typeof f);
    {
      assertSame("function", typeof f);
      function f(){}
      assertSame("function", typeof f);
    }
    assertSame("function", typeof f);
  `);
  assertSame("function", typeof f);
})();

// Legacy semantics used if function var-binding exists.
(function() {
  function f() { return 1; }
  eval(`
    assertSame(1, f());
    {
      assertSame(2, f());
      function f() { return 2; }
      assertSame(2, f());
    }
    assertSame(2, f());
  `);
  assertSame(2, f());
})();
