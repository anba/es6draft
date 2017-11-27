/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame,
} = Assert;

// 26.1.2 Reflect.construct: Add PrepareForTailCall ?
// https://bugs.ecmascript.org/show_bug.cgi?id=3693

function returnCaller() {
  return returnCaller.caller;
}

function testApply() {
  var result = Reflect.apply(returnCaller, null, []);
  assertSame(testApply, result);
}
testApply();

function testConstruct() {
  var result = Reflect.construct(returnCaller, []);
  assertNotSame(testApply, result);
  assertSame(returnCaller.prototype, Object.getPrototypeOf(result));
}
testConstruct();
