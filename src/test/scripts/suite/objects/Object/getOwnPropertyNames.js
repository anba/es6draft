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
  assertUndefined,
  assertNotUndefined,
  assertAccessorProperty,
  assertDataProperty,
} = Assert;


/* 19.1.3.7  Object.getOwnPropertyNames ( O ) */

assertBuiltinFunction(Object.getOwnPropertyNames, "getOwnPropertyNames", 1);

// functional changes in comparison to ES5.1
// - non-Object type input value coerced to Object
// - Symbol valued property keys not included

// Call ToObject() on input value
{
  let primitives = [true, false, 0, 1, "", "s", true, false, Symbol()];
  for (let v of primitives) {
    Object.getOwnPropertyNames(v);
  }
  assertThrows(TypeError, () => Object.getOwnPropertyNames(void 0));
  assertThrows(TypeError, () => Object.getOwnPropertyNames(null));

  // wrapped primitives do not have own properties...
  for (let v of [for (v of primitives) if (typeof v !== "string") v]) {
    let names = Object.getOwnPropertyNames(v);
    assertInstanceOf(Array, names);
    assertSame(0, names.length);
  }

  // ...except for 'length' and indexed properties on wrapped strings
  for (let v of [for (v of primitives) if (typeof v === "string") v]) {
    let names = Object.getOwnPropertyNames(v);
    assertInstanceOf(Array, names);
    assertSame(1 + v.length, names.length);
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
  let names = Object.getOwnPropertyNames(o);
  assertInstanceOf(Array, names);
  assertSame(0, names.length);
}

// Symbol valued property keys (2)
{
  let o = {
    [Symbol()]: 1,
    a: 0,
    ["b"]: 2,
  };
  let names = Object.getOwnPropertyNames(o);
  assertInstanceOf(Array, names);
  assertSame(2, names.length);
  names.sort();
  assertSame("a", names[0]);
  assertSame("b", names[1]);
}