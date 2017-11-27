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
    let
    yield
  }
  function g2() {
    let
    yield
    0
  }
  assertSyntaxError(`function g3() {
    let
    yield 0
  }`);
  function g4() {
    yield
    let
  }
  assertSyntaxError(`function g5() {
    yield let
  }`);
  function g6() {
    let yield
  }
}

// Strict mode, 'let' and 'yield' in ASI context in FunctionDeclaration
{
  var let = 1;
  assertSyntaxError(`function g1() {
    "use strict";
    let
    yield
  }`);
  assertSyntaxError(`function g2() {
    "use strict";
    let
    yield
    0
  }`);
  assertSyntaxError(`function g3() {
    "use strict";
    let
    yield 0
  }`);
  assertSyntaxError(`function g4() {
    "use strict";
    yield
    let
  }`);
  assertSyntaxError(`function g5() {
    "use strict";
    yield let
  }`);
  assertSyntaxError(`function g6() {
    "use strict";
    let yield
  }`);
}

// Non-strict mode, 'let' and 'yield' in ASI context in GeneratorDeclaration
{
  var let = 1;
  assertSyntaxError(`function* g1() {
    let
    yield
  }`);
  assertSyntaxError(`function* g2() {
    let
    yield
    0
  }`);
  assertSyntaxError(`function* g3() {
    let
    yield 0
  }`)
  function* g4() {
    yield
    let
  }
  function* g5() {
    yield let
  }
  assertSyntaxError(`function* g6() {
    let yield
  }`);

  assertEquals({value: void 0, done: false}, g4().next());
  assertEquals({value: 1, done: false}, g5().next());
}

// Strict mode, 'let' and 'yield' in ASI context in GeneratorDeclaration
{
  var let = 1;
  assertSyntaxError(`function* g1() {
    "use strict";
    let
    yield
  }`);
  assertSyntaxError(`function* g2() {
    "use strict";
    let
    yield
    0
  }`);
  assertSyntaxError(`function* g3() {
    "use strict";
    let
    yield 0
  }`);
  assertSyntaxError(`function* g4() {
    "use strict";
    yield
    let
  }`);
  assertSyntaxError(`function* g5() {
    "use strict";
    yield let
  }`);
  assertSyntaxError(`function* g6() {
    "use strict";
    let yield
  }`);
}
