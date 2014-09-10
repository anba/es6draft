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
  assertAccessorProperty,
  assertDataProperty,
} = Assert;


/* 19.1.3.3  Object.defineProperties ( O, Properties ) */

assertBuiltinFunction(Object.defineProperties, "defineProperties", 2);

// functional changes in comparison to ES5.1
// - Symbol valued property keys
// - Intermediate exceptions do not stop property traversal, first exception is reported

// Symbol valued property keys (1)
{
  let keyA = Symbol(), keyB = Symbol(), keyC = Symbol(), keyD = Symbol(),
      keyE = Symbol(), keyF = Symbol(), keyG = Symbol();
  let getter = () => {}, setter = () => {};
  let o = Object.defineProperties({}, {
    [keyA]: {},
    [keyB]: {value: "prop-b"},
    [keyC]: {value: "prop-c", writable: true, enumerable: true, configurable: true},
    [keyD]: {get: getter},
    [keyE]: {set: setter},
    [keyF]: {get: getter, set: setter},
    [keyG]: {get: getter, set: setter, enumerable: true, configurable: true},
  });
  assertDataProperty(o, keyA, {value: void 0, writable: false, enumerable: false, configurable: false});
  assertDataProperty(o, keyB, {value: "prop-b", writable: false, enumerable: false, configurable: false});
  assertDataProperty(o, keyC, {value: "prop-c", writable: true, enumerable: true, configurable: true});
  assertAccessorProperty(o, keyD, {get: getter, set: void 0, enumerable: false, configurable: false});
  assertAccessorProperty(o, keyE, {get: void 0, set: setter, enumerable: false, configurable: false});
  assertAccessorProperty(o, keyF, {get: getter, set: setter, enumerable: false, configurable: false});
  assertAccessorProperty(o, keyG, {get: getter, set: setter, enumerable: true, configurable: true});
}

// Symbol valued property keys (2)
{
  let key = Symbol();
  assertThrows(TypeError, () => Object.defineProperties({}, {
    [key]: {value: 1, get(){}}
  }));
  assertThrows(TypeError, () => Object.defineProperties({}, {
    [key]: {value: 1, set(){}}
  }));
  assertThrows(TypeError, () => Object.defineProperties({}, {
    [key]: {value: 1, get(){}, set(){}}
  }));
}

// Intermediate exceptions during install do not stop property traversal, first exception is reported (1)
{
  // Precondition: [[OwnKeys]] for Proxies can return the same key multiple times
  assertSame(2, [for (k of Reflect.ownKeys(new Proxy({}, {ownKeys: () => ["a", "a"]}))) k].length);

  let value = 0;
  let props = new Proxy({a: -1}, {
    get: () => ({value: value++}),
    ownKeys: () => ["a", "a", "a"]
  });
  let o = {};
  assertThrows(TypeError, () => Object.defineProperties(o, props));
  assertSame(3, value);
  assertDataProperty(o, "a", {value: 0, writable: false, enumerable: false, configurable: false});
}

// Intermediate exceptions during install do not stop property traversal, first exception is reported (2)
{
  let o = {};
  Object.defineProperty(o, "a", {value: 0, writable: false, enumerable: true, configurable: false});
  let props = new Proxy({
    a: {value: 1},
    b: {value: 1},
  }, {
    ownKeys: () => ["a", "b"]
  });
  assertThrows(TypeError, () => Object.defineProperties(o, props));
  assertDataProperty(o, "a", {value: 0, writable: false, enumerable: true, configurable: false});
  assertDataProperty(o, "b", {value: 1, writable: false, enumerable: false, configurable: false});
}

// Exceptions during property descriptor retrieval are reported immediately (1)
{
  class MyError extends Error {}
  let value = 0;
  let props = {
    get a() { value++; throw new MyError },
    get b() { value++; throw new MyError },
  };
  assertThrows(MyError, () => Object.defineProperties({}, props));
  assertSame(1, value);
}

// Exceptions during property descriptor retrieval are reported immediately (2)
{
  let value = 0;
  let props = {
    get a() { value++; return {value: void 0, get: void 0} },
    get b() { value++; return {value: void 0, get: void 0} },
  };
  assertThrows(TypeError, () => Object.defineProperties({}, props));
  assertSame(1, value);
}
