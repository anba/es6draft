/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertBuiltinFunction, assertThrows, assertInstanceOf, assertFalse, assertTrue
} = Assert;


/* Promise.prototype.then ( onFulfilled , onRejected ) */

assertBuiltinFunction(Promise.prototype.then, "then", 2);

// .then() called with value where IsPromise(value) = false, value is not a Promise Object
{
  for (let value of [void 0, null, 0, true, "", {}, [], () => {}]) {
    assertThrows(TypeError, () => Promise.prototype.then.call(value));
  }
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
  // undefined .constructor defaults to intrinsic %Promise%
  Object.assign(new Promise(() => {}), {constructor: void 0}).then();

  for (let constructor of [null, 0, ""]) {
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

// Does not throw TypeError if constructor returns a different object
{
  let thenCalled = false;
  let promise = new Promise(() => {});
  function Constructor(executor) {
    executor(() => {}, () => {});
    return {
      then() {
        assertFalse(thenCalled);
        thenCalled = true;
      }
    };
  }
  Constructor[Symbol.species] = Constructor;
  promise.constructor = Constructor;
  let thenPromise = promise.then();
  assertFalse(thenCalled);
  thenPromise.then();
  assertTrue(thenCalled);
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
