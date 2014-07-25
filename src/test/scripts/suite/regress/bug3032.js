/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertEquals, assertFalse, assertTrue, assertThrows, fail
} = Assert;

// 13.6.4.8 ForIn/OfBodyEvaluation: Unhandled abrupt completions in 3.f.iii.2 and 3.h.iv
// https://bugs.ecmascript.org/show_bug.cgi?id=3032

// Error from step 3.b not passed to iterator (1)
{
  const special = {};
  let iter = {
    [Symbol.iterator]() { return this; },
    next() { throw special; },
    throw() { fail `throw handler called`; }
  };
  let caught;
  try {
    for (let v of iter) fail `loop body entered`;
  } catch (e) { caught = e; }
  assertSame(special, caught);
}

// Error from step 3.b not passed to iterator (1)
{
  const special = {};
  let iter = {
    [Symbol.iterator]() { return this; },
    next() {
      return {
        get done() { throw special },
        get value() { fail `value getter invoked` }
      };
    },
    throw() { fail `throw handler called`; }
  };
  let caught;
  try {
    for (let v of iter) fail `loop body entered`;
  } catch (e) { caught = e; }
  assertSame(special, caught);
}

// Error from step 3.e not passed to iterator
{
  const special = {};
  let iter = {
    [Symbol.iterator]() { return this; },
    next() {
      return {
        get done() { return false },
        get value() { throw special }
      };
    },
    throw() { fail `throw handler called`; }
  };
  let caught;
  try {
    for (let v of iter) fail `loop body entered`;
  } catch (e) { caught = e; }
  assertSame(special, caught);
}

// Abrupt completion in 3.f.iii.2 intercepted
{
  let throwCalled = false, nextCalled = false;
  let iter = {
    [Symbol.iterator]() { return this; },
    next() {
      nextCalled = true;
      return {value: null, done: false};
    },
    throw() { throwCalled = true; }
  };
  assertThrows(() => {
    let v;
    for ({v} of iter) fail `loop body entered`
  }, TypeError);
  assertTrue(nextCalled);
  assertTrue(throwCalled);
}

// Abrupt completion in 3.h.iv intercepted
{
  let throwCalled = false, nextCalled = false;
  let iter = {
    [Symbol.iterator]() { return this; },
    next() {
      nextCalled = true;
      return {value: null, done: false};
    },
    throw() { throwCalled = true; }
  };
  assertThrows(() => {
    for (let {v} of iter) fail `loop body entered`
  }, TypeError);
  assertTrue(nextCalled);
  assertTrue(throwCalled);
}
