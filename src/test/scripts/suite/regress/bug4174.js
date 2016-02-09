/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows, fail
} = Assert;

// B.3.5 VariableStatements in Catch blocks: Incorrect redefinition of steps
// https://bugs.ecmascript.org/show_bug.cgi?id=4174

var e = "global";

assertThrows(SyntaxError, () => {
  {
    let e = 123;
    assertSame(123, e);
    eval("for (var e of []) ;");
  }
  fail `unreachable`;
});

assertThrows(SyntaxError, () => {
  {
    let e = 123;
    assertSame(123, e);
    eval("for (var e in {}) ;");
  }
  fail `unreachable`;
});

assertThrows(SyntaxError, () => {
  try { throw null } catch(e) {
    eval("for (var e of []) ;");
  }
  fail `unreachable`;
});

function catchVar() {
  try { throw null } catch(e) {
    assertSame(null, e);
    eval(`
      assertSame(null, e);
      var e = "ok";
      assertSame("ok", e);
    `);
    assertSame("ok", e);
  }
  assertSame(void 0, e);
}
catchVar();

function catchForInVar() {
  try { throw null } catch(e) {
    assertSame(null, e);
    eval(`
      assertSame(null, e);
      for (var e in {ok: 0}) ;
      assertSame("ok", e);
    `);
    assertSame("ok", e);
  }
  assertSame(void 0, e);
}
catchForInVar();

function catchForVar() {
  try { throw null } catch(e) {
    assertSame(null, e);
    eval(`
      assertSame(null, e);
      for (var e = "ok"; false;) ;
      assertSame("ok", e);
    `);
    assertSame("ok", e);
  }
  assertSame(void 0, e);
}
catchForVar();
