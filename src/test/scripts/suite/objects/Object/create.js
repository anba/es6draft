/*
 * Copyright (c) André Bargull
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


/* 19.1.3.2  Object.create ( O [, Properties] ) */

assertBuiltinFunction(Object.create, "create", 2);

// functional changes in comparison to ES5.1
// - Symbol valued property keys
// - Intermediate exceptions do not stop property traversal, first exception is reported

// Symbol valued property keys (1)
{
  let keyA = Symbol(), keyB = Symbol(), keyC = Symbol(), keyD = Symbol(),
      keyE = Symbol(), keyF = Symbol(), keyG = Symbol();
  let getter = () => {}, setter = () => {};
  let o = Object.create(null, {
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
  assertThrows(TypeError, () => Object.create(null, {
    [key]: {value: 1, get(){}}
  }));
  assertThrows(TypeError, () => Object.create(null, {
    [key]: {value: 1, set(){}}
  }));
  assertThrows(TypeError, () => Object.create(null, {
    [key]: {value: 1, get(){}, set(){}}
  }));
}

// Intermediate exceptions during install do not stop property traversal, first exception is reported
{
  // Precondition: [[OwnKeys]] for Proxies can return the same key multiple times
  assertThrows(TypeError, () => Reflect.ownKeys(new Proxy({}, {ownKeys: () => ["a", "a"]})));

  let value = 0;
  let props = new Proxy({a: -1}, {
    get: () => ({value: value++}),
    ownKeys: () => ["a", "a", "a"]
  });
  assertThrows(TypeError, () => Object.create(null, props));
  assertSame(0, value);
}

// Exceptions during property descriptor retrieval are reported immediately (1)
{
  class MyError extends Error {}
  let value = 0;
  let props = {
    get a() { value++; throw new MyError },
    get b() { value++; throw new MyError },
  };
  assertThrows(MyError, () => Object.create(null, props));
  assertSame(1, value);
}

// Exceptions during property descriptor retrieval are reported immediately (2)
{
  let value = 0;
  let props = {
    get a() { value++; return {value: void 0, get: void 0} },
    get b() { value++; return {value: void 0, get: void 0} },
  };
  assertThrows(TypeError, () => Object.create(null, props));
  assertSame(1, value);
}
