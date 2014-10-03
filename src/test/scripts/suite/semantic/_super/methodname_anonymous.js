/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertThrows, fail
} = Assert;

// Non-computed property names
{
  let obj = {
    __proto__: {
      a: () => "A",
      b: () => "B",
      c: () => "C",
    },
    a: function aaa() { return super() },
    b: function() { return super() },
    c: (0, function() { return super() }),
  };
  assertSame("A", obj.a());
  assertSame("B", obj.b());
  assertThrows(ReferenceError, () => obj.c());

  let home = {
    __proto__: {
      a: () => "A'",
      b: () => "B'",
      c: () => "C'",
    },
    a: () => { fail `called home.a` },
    b: () => { fail `called home.b` },
    c: () => { fail `called home.c` },
  };
  assertSame("A'", obj.a.toMethod(home)());
  assertSame("B'", obj.b.toMethod(home)());
  assertThrows(ReferenceError, () => obj.c.toMethod(home)());
  assertSame("C'", obj.c.toMethod(home, "c")());
}

// Computed property names, property name is String
{
  let obj = {
    __proto__: {
      a: () => "A",
      b: () => "B",
      c: () => "C",
    },
    ["a"]: function aaa() { return super() },
    ["b"]: function() { return super() },
    ["c"]: (0, function() { return super() }),
  };
  assertSame("A", obj.a());
  assertSame("B", obj.b());
  assertThrows(ReferenceError, () => obj.c());

  let home = {
    __proto__: {
      a: () => "A'",
      b: () => "B'",
      c: () => "C'",
    },
    a: () => { fail `called home.a` },
    b: () => { fail `called home.b` },
    c: () => { fail `called home.c` },
  };
  assertSame("A'", obj.a.toMethod(home)());
  assertSame("B'", obj.b.toMethod(home)());
  assertThrows(ReferenceError, () => obj.c.toMethod(home)());
  assertSame("C'", obj.c.toMethod(home, "c")());
}

// Computed property names, property name is Symbol
{
  let symA = Symbol("a"), symB = Symbol("b"), symC = Symbol("c");
  let obj = {
    __proto__: {
      [symA]: () => "A",
      [symB]: () => "B",
      [symC]: () => "C",
    },
    [symA]: function aaa() { return super() },
    [symB]: function() { return super() },
    [symC]: (0, function() { return super() }),
  };
  assertSame("A", obj[symA]());
  assertSame("B", obj[symB]());
  assertThrows(ReferenceError, () => obj[symC]());

  let home = {
    __proto__: {
      [symA]: () => "A'",
      [symB]: () => "B'",
      [symC]: () => "C'",
    },
    [symA]: () => { fail `called home.a` },
    [symB]: () => { fail `called home.b` },
    [symC]: () => { fail `called home.c` },
  };
  assertSame("A'", obj[symA].toMethod(home)());
  assertSame("B'", obj[symB].toMethod(home)());
  assertThrows(ReferenceError, () => obj[symC].toMethod(home)());
  assertSame("C'", obj[symC].toMethod(home, symC)());
}
