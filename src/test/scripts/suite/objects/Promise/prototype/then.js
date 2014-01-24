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
  let uninitPromiseWithConstructor = Object.defineProperty(Promise[Symbol.create](), "constructor", {
    get() { throw new ConstructorAccessed }, configurable: true
  });
  assertThrows(() => uninitPromiseWithConstructor.then(), TypeError);
}

// Accesses .constructor property
{
  class ConstructorAccessed extends Error {}
  let promiseWithConstructor = Object.defineProperty(new Promise(() => {}), "constructor", {
    get() { throw new ConstructorAccessed }, configurable: true
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

// Throws TypeError if constructor returns a different object
{
  let promise = new Promise(() => {});
  function Constructor(executor) {
    executor(() => {}, () => {});
    return {};
  }
  promise.constructor = Constructor;
  assertThrows(() => promise.then(), TypeError);
}

// Throws TypeError if executor is called multiple times (1)
{
  let promise = new Promise(() => {});
  function Constructor(executor) {
    // call executor() twice, second call triggers TypeError
    executor(() => {}, () => {});
    assertThrows(() => executor(() => {}, () => {}), TypeError);
    return this;
  }
  promise.constructor = Constructor;
  promise.then();
}

// Throws TypeError if executor is called multiple times (2)
{
  let promise = new Promise(() => {});
  function Constructor(executor) {
    // call executor() twice, second call triggers TypeError
    executor(null, null);
    assertThrows(() => executor(() => {}, () => {}), TypeError);
    return this;
  }
  promise.constructor = Constructor;
  assertThrows(() => promise.then(), TypeError);
}

// Throws TypeError if executor is called multiple times (3)
{
  let promise = new Promise(() => {});
  function Constructor(executor) {
    // call executor() twice, second call does not trigger TypeError
    executor(void 0, void 0);
    executor(() => {}, () => {})
    return this;
  }
  promise.constructor = Constructor;
  promise.then();
}
