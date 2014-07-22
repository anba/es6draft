/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertTrue, assertUndefined,
  assertSame, assertThrows,
} = Assert;

// MOP access on uninitialized typed arrays

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

// Test with concrete TypedArray constructor and normal function constructor
for (let constructor of [MyConstructor, Int8Array]) {
  function create() {
    return TypedArray[Symbol.create].call(constructor);
  }

  // [[GetPrototypeOf]]
  assertSame(constructor.prototype, Reflect.getPrototypeOf(create()));

  // [[DefineOwnProperty]]
  for (let p of integerIndexedProperties) {
    assertThrows(() => Reflect.defineProperty(create(), p, {value: 0}), TypeError);
  }
  for (let p of notIntegerIndexedProperties) {
    assertTrue(Reflect.defineProperty(create(), p, {value: 0}));
  }

  // [[GetOwnProperty]]
  // - Throw TypeError for integer indexed properties
  // - No TypeError if property is not integer indexed
  for (let p of integerIndexedProperties) {
    assertThrows(() => Reflect.getOwnPropertyDescriptor(create(), p), TypeError);
  }
  for (let p of notIntegerIndexedProperties) {
    assertUndefined(Reflect.getOwnPropertyDescriptor(create(), p));
  }

  // [[HasProperty]]
  // - Throw TypeError for integer indexed properties
  // - No TypeError if property is not integer indexed
  for (let p of integerIndexedProperties) {
    assertThrows(() => Reflect.has(create(), p), TypeError);
  }
  for (let p of notIntegerIndexedProperties) {
    assertFalse(Reflect.has(create(), p));
  }

  // [[Get]]
  // - Throw TypeError for integer indexed properties
  // - No TypeError if property is not integer indexed
  for (let p of integerIndexedProperties) {
    assertThrows(() => Reflect.get(create(), p), TypeError);
  }
  for (let p of notIntegerIndexedProperties) {
    assertUndefined(Reflect.get(create(), p));
  }

  // [[Set]]
  // - Throw TypeError for integer indexed properties
  // - No TypeError if property is not integer indexed
  for (let p of integerIndexedProperties) {
    assertThrows(() => Reflect.set(create(), p, 0), TypeError);
  }
  for (let p of notIntegerIndexedProperties) {
    assertTrue(Reflect.set(create(), p, 0));
  }

  // [[Delete]]
  // - Throw TypeError for integer indexed properties
  // - No TypeError if property is not integer indexed
  for (let p of integerIndexedProperties) {
    assertThrows(() => Reflect.deleteProperty(create(), p), TypeError);
  }
  for (let p of notIntegerIndexedProperties) {
    assertTrue(Reflect.deleteProperty(create(), p));
  }

  // HasOwnProperty
  // - Throw TypeError for integer indexed properties
  // - No TypeError if property is not integer indexed
  for (let p of integerIndexedProperties) {
    assertThrows(() => create().hasOwnProperty(p), TypeError);
  }
  for (let p of notIntegerIndexedProperties) {
    assertFalse(create().hasOwnProperty(p));
  }

  // [[Enumerate]]
  assertThrows(() => Reflect.enumerate(create()), TypeError);

  // [[OwnPropertyKeys]]
  assertThrows(() => Reflect.ownKeys(create()), TypeError);
}
