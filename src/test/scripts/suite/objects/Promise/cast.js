/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertBuiltinFunction,
  assertSame,
  assertNotSame,
  assertThrows,
  assertTrue,
} = Assert;


/* Promise.cast ( x ) */

assertBuiltinFunction(Promise.cast, "cast", 1);

// Promise.cast(x) returns x iff IsPromise(x) and SameValue(thisValue, x[[PromiseConstructor]])
{
  // not a Promise object
  for (let v of [undefined, null, true, 0, "", [], {}, () => {}, function(){}]) {
    assertNotSame(v, Promise.cast(v));
  }

  // uninitialised Promise object
  let uninitPromise = Promise[Symbol.create]();
  assertNotSame(uninitPromise, Promise.cast(uninitPromise));

  // initialised Promise object
  let initPromise = new Promise(() => {});
  assertSame(initPromise, Promise.cast(initPromise));

  function Ctor(r) {
    r(() => {}, () => {});
  }
  // initialised Promise object, non-native constructor
  let initPromiseCtor = Promise.call(Promise[Symbol.create].call(Ctor), () => {});
  assertSame(initPromiseCtor, Promise.cast.call(Ctor, initPromiseCtor));

  // initialised Promise object, mismatch for [[PromiseConstructor]]
  let initPromiseOtherCtor = Promise.call(Promise[Symbol.create].call(Ctor), () => {});
  assertNotSame(initPromiseOtherCtor, Promise.cast(initPromiseOtherCtor));
  let initPromiseOtherCtor2 = new Promise(() => {});
  assertNotSame(initPromiseOtherCtor2, Promise.cast.call(Ctor, initPromiseOtherCtor2));
}

// GetDeferred(C) is not executed when IsPromise(x) and SameValue(thisValue, x[[PromiseConstructor]]) (1)
{
  let throwError = false;
  class XError extends Error { }
  class XPromise extends Promise {
    constructor(...args) {
      if (throwError) {
        throw new XError();
      }
      throwError = true;
      super(...args);
    }
  }

  let p = new XPromise(() => {});
  assertTrue(throwError);
  assertSame(p, Promise.cast.call(XPromise, p));
  assertSame(p, XPromise.cast(p));
}

// GetDeferred(C) is not executed when IsPromise(x) and SameValue(thisValue, x[[PromiseConstructor]]) (2)
{
  let throwError = false;
  class XError extends Error { }
  function Ctor(r) {
    if (throwError) {
      throw new XError();
    }
    throwError = true;
    return Promise.call(this, r);
  }

  let p = Ctor.call(Promise[Symbol.create].call(Ctor), () => {});
  assertTrue(throwError);
  assertSame(p, Promise.cast.call(Ctor, p));
}

// Throws TypeError if thisValue is not a Constructor
{
  for (let v of [undefined, null, true, 0, "", [], {}, () => {}]) {
    assertThrows(() => Promise.cast.call(v), TypeError);
  }
}
