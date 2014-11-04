/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertBuiltinFunction,
  assertThrows,
  assertSame,
  assertFalse,
  assertAccessorProperty,
  assertDataProperty,
  fail,
} = Assert;


/* 19.1.3.5  Object.freeze ( O ) */

assertBuiltinFunction(Object.freeze, "freeze", 1);

// functional changes in comparison to ES5.1
// - non-Object type input value returns input instead of TypeError
// - Symbol valued property keys
// - Intermediate exceptions do not stop property traversal, first exception is reported

// Return input argument when non-Object
{
  let primitives = [void 0, null, true, false, 0, 1, "", "s", Symbol()];
  for (let v of primitives) {
    assertSame(v, Object.freeze(v));
  }
}

// Symbol valued property keys are frozen, too
{
  let keyA = Symbol(), keyB = Symbol(), keyC = Symbol(), keyD = Symbol(),
      keyE = Symbol(), keyF = Symbol(), keyG = Symbol();
  let getter = () => {}, setter = () => {};
  let o = {};
  Object.defineProperty(o, keyA, {});
  Object.defineProperty(o, keyB, {value: "prop-b"});
  Object.defineProperty(o, keyC, {value: "prop-c", writable: true, enumerable: true, configurable: true});
  Object.defineProperty(o, keyD, {get: getter});
  Object.defineProperty(o, keyE, {set: setter});
  Object.defineProperty(o, keyF, {get: getter, set: setter});
  Object.defineProperty(o, keyG, {get: getter, set: setter, enumerable: true, configurable: true});
  Object.freeze(o);
  assertDataProperty(o, keyA, {value: void 0, writable: false, enumerable: false, configurable: false});
  assertDataProperty(o, keyB, {value: "prop-b", writable: false, enumerable: false, configurable: false});
  assertDataProperty(o, keyC, {value: "prop-c", writable: false, enumerable: true, configurable: false});
  assertAccessorProperty(o, keyD, {get: getter, set: void 0, enumerable: false, configurable: false});
  assertAccessorProperty(o, keyE, {get: void 0, set: setter, enumerable: false, configurable: false});
  assertAccessorProperty(o, keyF, {get: getter, set: setter, enumerable: false, configurable: false});
  assertAccessorProperty(o, keyG, {get: getter, set: setter, enumerable: true, configurable: false});
}

// Test correct property traversal order, no additional MOP methods are called
{
  let expectedLog = ",preventExtensions,ownKeys,getOwnPropertyDescriptor:a,defineProperty:a,getOwnPropertyDescriptor:b,defineProperty:b";
  let log = "";
  let o = new Proxy({a: 1, b: 2}, new Proxy({
    defineProperty(t, pk, d) {
      log += `:${pk}`;
      return Object.defineProperty(t, pk, d);
    },
    getOwnPropertyDescriptor(t, pk) {
      log += `:${pk}`;
      return Object.getOwnPropertyDescriptor(t, pk);
    },
    ownKeys() {
      return ["a", "b"];
    },
  }, {
    get(t, pk, r) {
      log += "," + pk;
      return t[pk];
    }
  }));
  Object.freeze(o);
  assertSame(expectedLog, log);
  // Rinse and repeat...
  log = "";
  Object.freeze(o);
  assertSame(expectedLog, log);
}

// Intermediate exceptions do not stop property traversal, first exception is reported (1)
{
  class MyError extends Error {}
  let count = 0;
  let o = new Proxy({a: 1, b: 2}, {
    defineProperty(t, pk, d) {
      count++;
      if (pk === "a") {
        throw new MyError;
      }
      return Object.defineProperty(t, pk, d);
    },
    ownKeys: () => ["a", "b"]
  });
  assertThrows(MyError, () => Object.freeze(o));
  assertSame(2, count);
  assertDataProperty(o, "a", {value: 1, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "b", {value: 2, writable: false, enumerable: true, configurable: false});
  assertFalse(Object.isExtensible(o));
}

// Intermediate exceptions do not stop property traversal, first exception is reported (2)
{
  class MyError extends Error {}
  let count = 0, getOwnPDCalled = false;
  let o = new Proxy({a: 1, b: 2}, {
    defineProperty(t, pk, d) {
      count++;
      if (pk === "a") {
        fail `[[DefineProperty]] for a`;
      }
      return Object.defineProperty(t, pk, d);
    },
    getOwnPropertyDescriptor(t, pk) {
      if (pk === "a" && !getOwnPDCalled) {
        getOwnPDCalled = true;
        throw new MyError;
      }
      return Object.getOwnPropertyDescriptor(t, pk);
    },
    ownKeys: () => ["a", "b"]
  });
  assertThrows(MyError, () => Object.freeze(o));
  assertSame(1, count);
  assertDataProperty(o, "a", {value: 1, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "b", {value: 2, writable: false, enumerable: true, configurable: false});
  assertFalse(Object.isExtensible(o));
}

// Intermediate exceptions do not stop property traversal, first exception is reported (3)
{
  class MyError extends Error {}
  let count = 0;
  let o = new Proxy({a: 1, b: 2}, {
    defineProperty(t, pk, d) {
      count++;
      return Object.defineProperty(t, pk, d);
    },
    getOwnPropertyDescriptor(t, pk) {
      count++;
      return Object.getOwnPropertyDescriptor(t, pk);
    },
    ownKeys: () => { throw new MyError }
  });
  assertThrows(MyError, () => Object.freeze(o));
  assertSame(0, count);
  assertDataProperty(o, "a", {value: 1, writable: true, enumerable: true, configurable: true});
  assertDataProperty(o, "b", {value: 2, writable: true, enumerable: true, configurable: true});
  assertFalse(Object.isExtensible(o));
}
