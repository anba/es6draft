/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertBuiltinFunction, assertThrows, assertInstanceOf, assertFalse
} = Assert;


/* Promise.prototype.then ( onFulfilled , onRejected ) */

assertBuiltinFunction(Promise.prototype.then, "then", 2);

// .then() called with value where IsPromise(value) = false, value is not a Promise Object
{
  for (let value of [void 0, null, 0, true, "", {}, [], () => {}]) {
    assertThrows(TypeError, () => Promise.prototype.then.call(value));
  }
}

// .then() called with value where IsPromise(value) = false, value is a Promise Object
{
  // IsPromise() returns false if the argument is an uninitialized promise object
  let uninitPromise = new class extends Promise{ constructor(){ /* no super */ } };
  let onFulfilled = () => {};
  let onRejected = () => {};

  // Test with and without arguments
  assertThrows(TypeError, () => uninitPromise.then());
  assertThrows(TypeError, () => uninitPromise.then(onFulfilled, onRejected));
  assertThrows(TypeError, () => uninitPromise.then(0, 0));

  // Test .constructor property is not accessed before IsPromise() check
  class ConstructorAccessed extends Error {}
  let uninitPromiseWithConstructor = new class extends Promise{ constructor(){ /* no super */ } };
  Object.defineProperty(uninitPromiseWithConstructor, "constructor", {
    get() { throw new ConstructorAccessed }, configurable: true
  });
  assertThrows(TypeError, () => uninitPromiseWithConstructor.then());
}

// Accesses .constructor property
{
  class ConstructorAccessed extends Error {}
  let promiseWithConstructor = Object.defineProperty(new Promise(() => {}), "constructor", {
    get() { throw new ConstructorAccessed }, configurable: true
  });
  assertThrows(ConstructorAccessed, () => promiseWithConstructor.then());
}

// .constructor property is not a Constructor object
{
  for (let constructor of [null, void 0, 0, ""]) {
    let promise = Object.assign(new Promise(() => {}), {constructor});
    assertThrows(TypeError, () => promise.then());
  }
  for (let constructor of [() => {}, {}, []]) {
    assertFalse(Symbol.species in constructor);
    let promise = Object.assign(new Promise(() => {}), {constructor});
    assertInstanceOf(Promise, promise.then());
  }
  for (let constructor of [() => {}, {}, []]) {
    assertFalse(Symbol.species in constructor);
    constructor[Symbol.species] = constructor;
    let promise = Object.assign(new Promise(() => {}), {constructor});
    assertThrows(TypeError, () => promise.then());
  }
}

// Throws TypeError if constructor returns a different object
{
  let promise = new Promise(() => {});
  function Constructor(executor) {
    executor(() => {}, () => {});
    return {};
  }
  Constructor[Symbol.species] = Constructor;
  promise.constructor = Constructor;
  assertThrows(TypeError, () => promise.then());
}

// Throws TypeError if executor is called multiple times (1)
{
  let promise = new Promise(() => {});
  function Constructor(executor) {
    // call executor() twice, second call triggers TypeError
    executor(() => {}, () => {});
    assertThrows(TypeError, () => executor(() => {}, () => {}));
    return this;
  }
  Constructor[Symbol.species] = Constructor;
  promise.constructor = Constructor;
  promise.then();
}

// Throws TypeError if executor is called multiple times (2)
{
  let promise = new Promise(() => {});
  function Constructor(executor) {
    // call executor() twice, second call triggers TypeError
    executor(null, null);
    assertThrows(TypeError, () => executor(() => {}, () => {}));
    return this;
  }
  Constructor[Symbol.species] = Constructor;
  promise.constructor = Constructor;
  assertThrows(TypeError, () => promise.then());
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
  Constructor[Symbol.species] = Constructor;
  promise.constructor = Constructor;
  promise.then();
}
