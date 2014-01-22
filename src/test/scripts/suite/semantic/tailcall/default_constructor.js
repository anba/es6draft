/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// Default constructor is in tail-call position

function returnCaller() {
  return returnCaller.caller;
}

class tailCall extends returnCaller { }

function test() {
  let caller = tailCall();
  assertSame(test, caller);
}

test();
