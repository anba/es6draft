/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, fail,
} = Assert;

// Clone resolve
{
  let called = false, resolveCalled = false;
  function F(executor) {
    called = true;
    let clone = executor.toMethod({});
    clone(() => { resolveCalled = true }, () => fail `reject called`);
  }
  Promise.resolve.call(F);
  assertTrue(called);
  assertTrue(resolveCalled);
}

// Clone reject
{
  let called = false, rejectCalled = false;
  function F(executor) {
    called = true;
    let clone = executor.toMethod({});
    clone(() => fail `resolve called`, () => { rejectCalled = true });
  }
  Promise.reject.call(F);
  assertTrue(called);
  assertTrue(rejectCalled);
}
