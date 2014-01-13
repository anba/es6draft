/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertBuiltinFunction, assertThrows
} = Assert;


/* Promise.prototype.then ( onFulfilled , onRejected ) */

assertBuiltinFunction(Promise.prototype.then, "then", 2);

// .then() called with value where IsPromise(value) = false, value is not a Promise Object
{
  for (let value of [void 0, null, 0, true, "", {}, [], () => {}]) {
    assertThrows(() => Promise.prototype.then.call(value), TypeError);
  }
}

// .then() called with value where IsPromise(value) = false, value is a Promise Object
{
  // IsPromise() returns false if the argument is an uninitialised promise object
  let uninitPromise = Promise[Symbol.create]();
  let onFulfilled = () => {};
  let onRejected = () => {};

  // Test with and without arguments
  assertThrows(() => uninitPromise.then(), TypeError);
  assertThrows(() => uninitPromise.then(onFulfilled, onRejected), TypeError);
  assertThrows(() => uninitPromise.then(0, 0), TypeError);

  // Test .constructor property is not accessed before IsPromise() check
  class ConstructorAccessed extends Error {}
  let uninitPromiseWithConstructor = Object.mixin(Promise[Symbol.create](), {
    get constructor() {
      throw new ConstructorAccessed
    }
  });
  assertThrows(() => uninitPromiseWithConstructor.then(), TypeError);
}

// Accesses .constructor property
{
  class ConstructorAccessed extends Error {}
  let promiseWithConstructor = Object.mixin(new Promise(() => {}), {
    get constructor() {
      throw new ConstructorAccessed
    }
  });
  assertThrows(() => promiseWithConstructor.then(), ConstructorAccessed);
}

// .constructor property is not a Constructor object
{
  for (let constructor of [null, void 0, 0, "", () => {}, {}, []]) {
    let promise = Object.assign(new Promise(() => {}), {constructor});
    assertThrows(() => promise.then(), TypeError);
  }
}

// 
{
  let promise = new Promise(() => {});
  function Constructor(resolver) {
    let p = {};
    // call resolver() twice
    resolver(() => {}, () => {});
    resolver(() => {}, () => {});
    return p;
  }
  promise.constructor = Constructor;
  promise.then();
}
