/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertBuiltinFunction,
  assertThrows,
  assertSame,
  assertInstanceOf,
  assertTrue,
} = Assert;


/* 19.1.3.8  Object.getOwnPropertySymbols ( O ) */

assertBuiltinFunction(Object.getOwnPropertySymbols, "getOwnPropertySymbols", 1);

// Call ToObject() on input value
{
  let primitives = [true, false, 0, 1, "", "s", true, false, Symbol()];
  for (let v of primitives) {
    Object.getOwnPropertySymbols(v);
  }
  assertThrows(TypeError, () => Object.getOwnPropertySymbols(void 0));
  assertThrows(TypeError, () => Object.getOwnPropertySymbols(null));

  // wrapped primitives do not have own symbol-valued properties
  for (let v of primitives) {
    let names = Object.getOwnPropertySymbols(v);
    assertInstanceOf(Array, names);
    assertSame(0, names.length);
  }
}

// Symbol valued property keys (1)
{
  let keyA = Symbol(), keyB = Symbol(), keyC = Symbol();
  let o = {
    [keyA]: 1,
    get [keyB]() {},
    get [keyC]() {},
    set [keyC](v) {},
  };
  let names = Object.getOwnPropertySymbols(o);
  assertInstanceOf(Array, names);
  assertSame(3, names.length);
  assertTrue(names.indexOf(keyA) != -1);
  assertTrue(names.indexOf(keyB) != -1);
  assertTrue(names.indexOf(keyC) != -1);
}

// Symbol valued property keys (2)
{
  let keyA = Symbol();
  let o = {
    [keyA]: 1,
    a: 0,
    ["b"]: 2,
  };
  let names = Object.getOwnPropertySymbols(o);
  assertInstanceOf(Array, names);
  assertSame(1, names.length);
  assertSame(keyA, names[0]);
}

// Symbol valued property keys (3)
{
  let keyA = Symbol(), keyB = Symbol();
  let o = {
    __proto__: {
      [keyB]: 2,
    },
    [keyA]: 1,
  };
  let names = Object.getOwnPropertySymbols(o);
  assertInstanceOf(Array, names);
  assertSame(1, names.length);
  assertSame(keyA, names[0]);
}
