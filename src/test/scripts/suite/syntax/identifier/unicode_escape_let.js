/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSyntaxError, assertSame
} = Assert;


// \u006cet is "let"
assertSame(`\u006cet`, "let");


// let declaration with binding identifier without ASI
(function testNoEscape() {
  var let = -1;
  let a = 0;
  {
    let a = 1;
    assertSame(1, a);
  }
  assertSame(0, a);
  assertSame(-1, let);
})();
assertSyntaxError(`\\u006cet a = 1`);


// let declaration with binding identifier with ASI
(function testNoEscape() {
  var let = -1;
  let a = 0;
  {
    let
    a = 1;
    assertSame(1, a);
  }
  assertSame(0, a);
  assertSame(-1, let);
})();
(function testEscape() {
  var let = -1;
  let a = 0;
  {
    \u006cet
    a = 1;
    assertSame(1, a);
  }
  assertSame(1, a);
  assertSame(-1, let);
})();


// let declaration with array binding pattern without ASI
(function testNoEscape() {
  var let = [-1];
  let a = 0;
  {
    let [a] = [1];
    assertSame(1, a);
  }
  assertSame(0, a);
  assertSame(-1, let[0]);
})();
(function testEscape() {
  var let = [-1];
  let a = 0;
  {
    \u006cet [a] = 1;
    assertSame(0, a);
  }
  assertSame(0, a);
  assertSame(1, let[0]);
})();


// let declaration with array binding pattern with ASI
(function testNoEscape() {
  var let = [-1];
  let a = 0;
  {
    let
    [a] = [1];
    assertSame(1, a);
  }
  assertSame(0, a);
  assertSame(-1, let[0]);
})();
(function testEscape() {
  var let = [-1];
  let a = 0;
  {
    \u006cet
    [a] = 1;
    assertSame(0, a);
  }
  assertSame(0, a);
  assertSame(1, let[0]);
})();
