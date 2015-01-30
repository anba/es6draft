/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue, assertThrows
} = Assert;

// Default constructor is not in tail-call position

var returnCallerCount = 0;

function returnCaller() {
  returnCallerCount += 1;
  return returnCaller.caller;
}

class tailCall extends returnCaller { }

function testCall() {
  assertSame(0, returnCallerCount);
  assertThrows(TypeError, () => tailCall());
  assertSame(0, returnCallerCount);
}
testCall();

function testConstruct() {
  assertSame(0, returnCallerCount);
  let caller = new tailCall();
  assertSame(1, returnCallerCount);
  // returnCaller() is called from a strict-mode function, that means returnCaller.caller is `null`,
  // and therefore a new object is returned with its prototype set to tailCall.prototype.
  assertSame(tailCall.prototype, Object.getPrototypeOf(caller));
}
testConstruct();
