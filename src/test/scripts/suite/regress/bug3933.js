/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertSyntaxError,
} = Assert;

// 13.6 Iteration Statements: Restore lookahead restriction "let [" in for-in statement to avoid shift/reduce conflict
// https://bugs.ecmascript.org/show_bug.cgi?id=3933

function forLoop() {
  var let;
  for (let = "ok"; false;) ;
  return let;
}
assertSame("ok", forLoop());

function forIn() {
  var let;
  for (let in {ok: 0}) ;
  return let;
}
assertSame("ok", forIn());

assertSyntaxError(`
  for (let of []) ;
`);

