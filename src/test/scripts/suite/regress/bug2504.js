/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertSyntaxError
} = Assert;

// 14.2 Arrow Function: Add [Yield] production parameter to ArrowFunction and ArrowParameters
// https://bugs.ecmascript.org/show_bug.cgi?id=2504

assertSyntaxError(`
  function* g() {
    var f1 = yield => { };
  }
`);

assertSyntaxError(`
function* g() {
  var f2 = (yield) => { };
}
`);

assertSyntaxError(`
function* g() {
  var f3 = (a, yield) => { };
}
`);

assertSyntaxError(`
function* g() {
  var f4 = (yield, a) => { };
}
`);
