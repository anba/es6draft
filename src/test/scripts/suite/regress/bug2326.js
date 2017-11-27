/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 14.6.2.2: ConditionalExpression does not propagate tail-call test and wrong return type
// https://bugs.ecmascript.org/show_bug.cgi?id=2326

function returnCaller(argument) {
  return {caller: returnCaller.caller, argument};
}
function tailCall(c) {
  "use strict";
  return c ? returnCaller(1) : returnCaller(0);
}
function test(c) {
  let {caller, argument} = tailCall(c);
  assertSame(test, caller);
  assertSame(c, argument); 
}
test(0);
test(1);
