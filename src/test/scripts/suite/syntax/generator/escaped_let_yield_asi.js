/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError, assertEquals
} = Assert;

// Non-strict mode, 'let' and 'yield' in ASI context in FunctionDeclaration
{
  var let = 1;
  function g1() {
    l\u0065t
    yield
  }
  function g2() {
    l\u0065t
    yield
    0
  }
  assertSyntaxError(String.raw`function g3() {
    l\u0065t
    yield 0
  }`);
  function g4() {
    yield
    l\u0065t
  }
  assertSyntaxError(String.raw`function g5() {
    yield l\u0065t
  }`);
  assertSyntaxError(String.raw`function g6() {
    l\u0065t yield
  }`);
}

// Strict mode, 'let' and 'yield' in ASI context in FunctionDeclaration
{
  var let = 1;
  assertSyntaxError(String.raw`function g1() {
    "use strict";
    l\u0065t
    yield
  }`);
  assertSyntaxError(String.raw`function g2() {
    "use strict";
    l\u0065t
    yield
    0
  }`);
  assertSyntaxError(String.raw`function g3() {
    "use strict";
    l\u0065t
    yield 0
  }`);
  assertSyntaxError(String.raw`function g4() {
    "use strict";
    yield
    l\u0065t
  }`);
  assertSyntaxError(String.raw`function g5() {
    "use strict";
    yield l\u0065t
  }`);
  assertSyntaxError(String.raw`function g6() {
    "use strict";
    l\u0065t yield
  }`);
}

// Non-strict mode, 'let' and 'yield' in ASI context in GeneratorDeclaration
{
  var let = 1;
  function* g1() {
    l\u0065t
    yield
  }
  function* g2() {
    l\u0065t
    yield
    0
  }
  function* g3() {
    l\u0065t
    yield 0
  }
  function* g4() {
    yield
    l\u0065t
  }
  function* g5() {
    yield l\u0065t
  }
  assertSyntaxError(String.raw`function* g6() {
    l\u0065t yield
  }`);

  assertEquals({value: void 0, done: false}, g1().next());
  assertEquals({value: void 0, done: false}, g2().next());
  assertEquals({value: 0, done: false}, g3().next());
  assertEquals({value: void 0, done: false}, g4().next());
  assertEquals({value: 1, done: false}, g5().next());
}

// Strict mode, 'let' and 'yield' in ASI context in GeneratorDeclaration
{
  var let = 1;
  assertSyntaxError(String.raw`function* g1() {
    "use strict";
    l\u0065t
    yield
  }`);
  assertSyntaxError(String.raw`function* g2() {
    "use strict";
    l\u0065t
    yield
    0
  }`);
  assertSyntaxError(String.raw`function* g3() {
    "use strict";
    l\u0065t
    yield 0
  }`);
  assertSyntaxError(String.raw`function* g4() {
    "use strict";
    yield
    l\u0065t
  }`);
  assertSyntaxError(String.raw`function* g5() {
    "use strict";
    yield l\u0065t
  }`);
  assertSyntaxError(String.raw`function* g6() {
    "use strict";
    l\u0065t yield
  }`);
}
