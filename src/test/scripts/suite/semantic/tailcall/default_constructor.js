/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertUndefined
} = Assert;

// Default constructor is not in tail-call position and returns `undefined`

function returnCaller() {
  return returnCaller.caller;
}

class tailCall extends returnCaller { }

function test() {
  let caller = tailCall();
  assertUndefined(caller);
}

test();
