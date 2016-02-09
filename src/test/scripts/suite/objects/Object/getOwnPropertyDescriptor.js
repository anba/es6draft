/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertBuiltinFunction,
  assertThrows,
  assertSame,
  assertUndefined,
  assertNotUndefined,
  assertAccessorProperty,
  assertDataProperty,
} = Assert;


/* 19.1.3.6  Object.getOwnPropertyDescriptor ( O, P ) */

assertBuiltinFunction(Object.getOwnPropertyDescriptor, "getOwnPropertyDescriptor", 2);

// functional changes in comparison to ES5.1
// - non-Object type input value coerced to Object
// - Symbol valued property keys

// Call ToObject() on input value
{
  let primitives = [true, false, 0, 1, "", "s", true, false, Symbol()];
  for (let v of primitives) {
    Object.getOwnPropertyDescriptor(v);
  }
  assertThrows(TypeError, () => Object.getOwnPropertyDescriptor(void 0));
  assertThrows(TypeError, () => Object.getOwnPropertyDescriptor(null));

  // wrapped primitives do not have own properties...
  for (let [v, p] of [for (v of primitives) for (p of ["valueOf", "toString"]) [v, p]]) {
    assertUndefined(Object.getOwnPropertyDescriptor(v, p));
  }

  // ...except for 'length' and indexed properties on wrapped strings
  assertNotUndefined(Object.getOwnPropertyDescriptor("", "length"));
  assertDataProperty("", "length", {value: 0, writable: false, enumerable: false, configurable: false});
  assertDataProperty("str", "length", {value: 3, writable: false, enumerable: false, configurable: false});
  assertUndefined(Object.getOwnPropertyDescriptor("", "0"));
  assertDataProperty("str", "0", {value: "s", writable: false, enumerable: true, configurable: false});
}

// Symbol valued property keys
{
  let keyA = Symbol(), keyB = Symbol(), keyC = Symbol();
  let getter = () => {}, setter = () => {};
  let o = {
    [keyA]: 1,
    get [keyB]() {},
    get [keyC]() {},
    set [keyC](v) {},
  };
  assertDataProperty(o, keyA, {value: 1, writable: true, enumerable: true, configurable: true});

  let descB = Object.getOwnPropertyDescriptor(o, keyB);
  assertNotUndefined(descB);
  assertNotUndefined(descB.get);
  assertAccessorProperty(o, keyB, {get: descB.get, set: void 0, enumerable: true, configurable: true});

  let descC = Object.getOwnPropertyDescriptor(o, keyC);
  assertNotUndefined(descC);
  assertNotUndefined(descC.get);
  assertNotUndefined(descC.set);
  assertAccessorProperty(o, keyC, {get: descC.get, set: descC.set, enumerable: true, configurable: true});
}
