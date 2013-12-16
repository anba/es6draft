/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertThrows,
  assertTrue,
  assertFalse,
  assertNotConstructor,
} = Assert;

// function object without [[Code]] throws TypeError
{
  class Fn extends Function { constructor() { } }

  let fn = new Fn();
  assertThrows(() => fn(), TypeError);
  assertThrows(() => new fn, TypeError);
}

// function object without [[Code]] does not call @@create
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
  assertNotConstructor(fn);
  assertThrows(() => new fn, TypeError);
  assertFalse(createCalled);
}
