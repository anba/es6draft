/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows, assertFalse, assertTrue, fail,
} = Assert;

// Base test (1)
{
  let called = false, resolveCalled = false;
  function F(executor) {
    called = true;
    executor(() => { resolveCalled = true }, () => fail `reject called`);
  }
  Promise.resolve.call(F);
  assertTrue(called);
  assertTrue(resolveCalled);
}

// Base test (2)
{
  let called = false, resolveCalled = false;
  function F(executor) {
    called = true;
    executor(void 0, void 0);
    executor(() => { resolveCalled = true }, () => fail `reject called`);
  }
  Promise.resolve.call(F);
  assertTrue(called);
  assertTrue(resolveCalled);
}

// Base test (3)
{
  let called = false, resolveCalled = false;
  function F(executor) {
    called = true;
    executor(() => { resolveCalled = true }, () => fail `reject called`);
    executor(void 0, void 0);
  }
  assertThrows(TypeError, () => Promise.resolve.call(F));
  assertTrue(called);
  assertFalse(resolveCalled);
}
