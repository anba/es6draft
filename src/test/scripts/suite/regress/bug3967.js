/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 18.2.1.2 EvalDeclarationInstantiation: WebCompat and lexical redeclaration checks in with/catch blocks
// https://bugs.ecmascript.org/show_bug.cgi?id=3967

var a = "global";

function withStatement() {
  var obj = {a: 0};

  assertSame(0, obj.a);
  assertSame("global", a);
  with(obj) eval("var a = 1");
  assertSame(1, obj.a);
  assertSame(void 0, a);
}
withStatement();

function catchClause() {
  assertSame("global", a);
  try {
    throw 0;
  } catch (a) {
    assertSame(0, a);
    eval("var a = 1");
    assertSame(1, a);
  }
  assertSame(void 0, a);
}
catchClause();
