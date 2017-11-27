/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, fail
} = Assert;

function g() { return "global"; }

function doExprInDefaultParams(
  a = do { function g() { return "do-expr"; } g },
  b = do { { function g() { return "do-expr"; } g } },
  c = do { { function g() { return "do-expr"; } } g },
  d = do { g() },
  e = do { () => g() }
) {
  assertSame("global", g());
  assertSame("do-expr", a());
  assertSame("do-expr", b());
  assertSame("global", c());
  assertSame("global", d);
  assertSame("global", e());
}
doExprInDefaultParams();


function doExprInBody() {
  var a = do {
    function g() { return "do-expr"; }
    g;
  };
  var b = do { {
    function g() { return "do-expr"; }
    g;
  } };
  var c = do {
    { function g() { return "do-expr"; } }
    g;
  };
  var d = do { g() };
  var e = do { () => g() };
  assertSame("global", g());
  assertSame("do-expr", a());
  assertSame("do-expr", b());
  assertSame("global", c());
  assertSame("global", d);
  assertSame("global", e());
}
doExprInBody();
