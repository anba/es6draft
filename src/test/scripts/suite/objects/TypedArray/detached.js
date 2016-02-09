/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue, assertUndefined,
  assertSame, assertThrows, assertEquals,
} = Assert;

// MOP access on detached typed arrays

const integerIndexedPropertiesValidZero = [
  -0, +0, "0",
];

const integerIndexedProperties = [
  -0, +0, "-0", "0",
  -1, +1, "-1", "1",
  -1.5, 1.5,
  "9999999999999998",
  0 / 0,
  -Infinity, +Infinity, "-Infinity",
];

const notIntegerIndexedProperties = [
  "blub",
  "0.", "0.0", "+0",
  "1.", "1.0", "+1",
  "9999999999999999",
  "+Infinity",
];

function MyConstructor() { }
const TypedArray = Object.getPrototypeOf(Int8Array);

// Test with concrete TypedArray constructors
for (let constructor of [Int8Array]) {
  function create(length = 0) {
    let array = new constructor(length);
    detachArrayBuffer(array.buffer);
    return array;
  }

  // [[GetPrototypeOf]]
  assertSame(constructor.prototype, Reflect.getPrototypeOf(create()));

  // [[DefineOwnProperty]]
  for (let p of integerIndexedPropertiesValidZero) {
    assertThrows(TypeError, () => Reflect.defineProperty(create(1), p, {value: 0}));
  }
  for (let p of integerIndexedProperties) {
    assertFalse(Reflect.defineProperty(create(), p, {value: 0}));
  }
  for (let p of notIntegerIndexedProperties) {
    assertTrue(Reflect.defineProperty(create(), p, {value: 0}));
  }

  // [[GetOwnProperty]]
  // - Throw TypeError for integer indexed properties
  // - No TypeError if property is not integer indexed
  for (let p of integerIndexedProperties) {
    assertThrows(TypeError, () => Reflect.getOwnPropertyDescriptor(create(), p));
  }
  for (let p of notIntegerIndexedProperties) {
    assertUndefined(Reflect.getOwnPropertyDescriptor(create(), p));
  }

  // [[HasProperty]]
  // - Throw TypeError for integer indexed properties
  // - No TypeError if property is not integer indexed
  for (let p of integerIndexedProperties) {
    assertThrows(TypeError, () => Reflect.has(create(), p));
  }
  for (let p of notIntegerIndexedProperties) {
    assertFalse(Reflect.has(create(), p));
  }

  // [[Get]]
  // - Throw TypeError for integer indexed properties
  // - No TypeError if property is not integer indexed
  for (let p of integerIndexedProperties) {
    assertThrows(TypeError, () => Reflect.get(create(), p));
  }
  for (let p of notIntegerIndexedProperties) {
    assertUndefined(Reflect.get(create(), p));
  }

  // [[Set]]
  // - Throw TypeError for integer indexed properties
  // - No TypeError if property is not integer indexed
  for (let p of integerIndexedProperties) {
    assertThrows(TypeError, () => Reflect.set(create(), p, 0));
  }
  for (let p of notIntegerIndexedProperties) {
    assertTrue(Reflect.set(create(), p, 0));
  }

  // [[Delete]]
  // - Throw TypeError for integer indexed properties
  // - No TypeError if property is not integer indexed
  for (let p of integerIndexedProperties) {
    assertThrows(TypeError, () => Reflect.deleteProperty(create(), p));
  }
  for (let p of notIntegerIndexedProperties) {
    assertTrue(Reflect.deleteProperty(create(), p));
  }

  // HasOwnProperty
  // - Throw TypeError for integer indexed properties
  // - No TypeError if property is not integer indexed
  for (let p of integerIndexedProperties) {
    assertThrows(TypeError, () => create().hasOwnProperty(p));
  }
  for (let p of notIntegerIndexedProperties) {
    assertFalse(create().hasOwnProperty(p));
  }

  // [[Enumerate]]
  assertEquals([], [...Reflect.enumerate(create())]);
  assertEquals(["0"], [...Reflect.enumerate(create(1))]);

  // [[OwnPropertyKeys]]
  assertEquals([], Reflect.ownKeys(create()));
  assertEquals(["0"], Reflect.ownKeys(create(1)));
}
