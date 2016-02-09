/*
 * Copyright (c) 2012-2015 André Bargull
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
    yi\u0065ld
  }
  function g2() {
    let
    yi\u0065ld
    0
  }
  assertSyntaxError(String.raw`function g3() {
    let
    yi\u0065ld 0
  }`);
  function g4() {
    yi\u0065ld
    let
  }
  assertSyntaxError(String.raw`function g5() {
    yi\u0065ld let
  }`);
  function g6() {
    let yi\u0065ld
  }
}

// Strict mode, 'let' and 'yield' in ASI context in FunctionDeclaration
{
  var let = 1;
  assertSyntaxError(String.raw`function g1() {
    "use strict";
    let
    yi\u0065ld
  }`);
  assertSyntaxError(String.raw`function g2() {
    "use strict";
    let
    yi\u0065ld
    0
  }`);
  assertSyntaxError(String.raw`function g3() {
    "use strict";
    let
    yi\u0065ld 0
  }`);
  assertSyntaxError(String.raw`function g4() {
    "use strict";
    yi\u0065ld
    let
  }`);
  assertSyntaxError(String.raw`function g5() {
    "use strict";
    yi\u0065ld let
  }`);
  assertSyntaxError(String.raw`function g6() {
    "use strict";
    let yi\u0065ld
  }`);
}

// Non-strict mode, 'let' and 'yield' in ASI context in GeneratorDeclaration
{
  var let = 1;
  assertSyntaxError(String.raw`function* g1() {
    let
    yi\u0065ld
  }`);
  assertSyntaxError(String.raw`function* g2() {
    let
    yi\u0065ld
    0
  }`);
  assertSyntaxError(String.raw`function* g3() {
    let
    yi\u0065ld 0
  }`);
  assertSyntaxError(String.raw`function* g4() {
    yi\u0065ld
    let
  }`);
  assertSyntaxError(String.raw`function* g5() {
    yi\u0065ld let
  }`);
  assertSyntaxError(String.raw`function* g6() {
    let yi\u0065ld
  }`);
}

// Strict mode, 'let' and 'yield' in ASI context in GeneratorDeclaration
{
  var let = 1;
  assertSyntaxError(String.raw`function* g1() {
    "use strict";
    let
    yi\u0065ld
  }`);
  assertSyntaxError(String.raw`function* g2() {
    "use strict";
    let
    yi\u0065ld
    0
  }`);
  assertSyntaxError(String.raw`function* g3() {
    "use strict";
    let
    yi\u0065ld 0
  }`);
  assertSyntaxError(String.raw`function* g4() {
    "use strict";
    yi\u0065ld
    let
  }`);
  assertSyntaxError(String.raw`function* g5() {
    "use strict";
    yi\u0065ld let
  }`);
  assertSyntaxError(String.raw`function* g6() {
    "use strict";
    let yi\u0065ld
  }`);
}
