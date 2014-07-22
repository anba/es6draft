/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows,
  assertTrue,
  assertFalse,
  assertConstructor,
} = Assert;

// function object without [[Code]] throws TypeError
{
  class Fn extends Function { constructor() { } }

  let fn = new Fn();
  assertThrows(() => fn(), TypeError);
  assertThrows(() => new fn, TypeError);
}

// function object without [[Code]] throws TypeError
{
  let createCalled = false;
  class Fn extends Function {
    constructor() { }
    [Symbol.create]() {
      createCalled = true;
      return {};
    }
  }

  let fn = new Fn();
  assertFalse(createCalled);
  assertConstructor(fn);
  assertThrows(() => new fn, TypeError);
  assertFalse(createCalled);
}
