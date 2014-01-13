/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertBuiltinFunction,
  assertThrows,
  assertAccessorProperty,
  assertDataProperty,
} = Assert;


/* 19.1.3.4  Object.defineProperty ( O, P, Attributes ) */

assertBuiltinFunction(Object.defineProperty, "defineProperty", 3);

// functional changes in comparison to ES5.1
// - Symbol valued property keys

// Symbol valued property keys (1)
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
  assertThrows(() => Object.defineProperty({}, key, {value: 1, get(){}}), TypeError);
  assertThrows(() => Object.defineProperty({}, key, {value: 1, set(){}}), TypeError);
  assertThrows(() => Object.defineProperty({}, key, {value: 1, get(){}, set(){}}), TypeError);
}
