/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows, fail
} = Assert;

// Annex E: B.3.5 and FunctionDeclaration in eval code
// https://bugs.ecmascript.org/show_bug.cgi?id=4339

const e = "outer";

// No SyntaxError
function functionDecl() {
  assertSame("outer", e);
  try {
    throw null;
  } catch (e) {
    assertSame(null, e);
    eval("function e() { }");
    assertSame(null, e);
  }
  assertSame("function", typeof e);
}
functionDecl();

assertThrows(SyntaxError, () => {
  try {
    throw null;
  } catch (e) {
    eval("fail('unreachable'); for (var e of []) ;");
  }
});
