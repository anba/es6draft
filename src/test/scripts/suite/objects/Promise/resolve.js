/*
 * Copyright (c) André Bargull
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


/* Promise.resolve ( x ) */

assertBuiltinFunction(Promise.resolve, "resolve", 1);

// Promise.resolve(x) returns x iff IsPromise(x) and SameValue(thisValue, x[[PromiseConstructor]])
{
  // not a Promise object
  for (let v of [undefined, null, true, 0, "", [], {}, () => {}, function(){}]) {
    assertNotSame(v, Promise.resolve(v));
  }

  // initialized Promise object
  let initPromise = new Promise(() => {});
  assertSame(initPromise, Promise.resolve(initPromise));

  class Ctor extends Promise {
    constructor(...args) {
      super(...args);
    }
  }
  // initialized Promise object, non-native constructor
  let initPromiseCtor = new Ctor(() => {});
  assertSame(initPromiseCtor, Promise.resolve.call(Ctor, initPromiseCtor));

  // initialized Promise object, mismatch for [[PromiseConstructor]]
  let initPromiseOtherCtor = new Ctor(() => {});
  assertNotSame(initPromiseOtherCtor, Promise.resolve(initPromiseOtherCtor));
  let initPromiseOtherCtor2 = new Promise(() => {});
  assertNotSame(initPromiseOtherCtor2, Promise.resolve.call(Ctor, initPromiseOtherCtor2));
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
  assertSame(p, Promise.resolve.call(XPromise, p));
  assertSame(p, XPromise.resolve(p));
}

// GetDeferred(C) is not executed when IsPromise(x) and SameValue(thisValue, x[[PromiseConstructor]]) (2)
{
  let throwError = false;
  class XError extends Error { }
  class Ctor extends Promise {
    constructor(...args) {
      if (throwError) {
        throw new XError();
      }
      throwError = true;
      return super(...args);
    }
  }

  let p = new Ctor(() => {});
  assertTrue(throwError);
  assertSame(p, Promise.resolve.call(Ctor, p));
}

// Throws TypeError if thisValue is not a Constructor
{
  for (let v of [undefined, null, true, 0, "", [], {}, () => {}]) {
    assertThrows(TypeError, () => Promise.resolve.call(v));
  }
}
