/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
  assertEquals,
} = Assert;

function enumerableNames(o) {
  return [...{[Symbol.iterator]: () => Reflect.enumerate(Object(o))}];
}

function getOwnKeys(o) {
  return Reflect.ownKeys(Object(o));
}

function* range(start, end) {
  for (; start < end; ++start) {
    yield "" + start;
  }
}

// [[Enumerate]] reports integer indexed, followed by string properties
for (let s of ["", new String("")]) {
  let names = enumerableNames(s);

  assertEquals([], names);
}

for (let s of ["abc", new String("abc")]) {
  let names = enumerableNames(s);

  assertEquals(["0","1","2"], names);
}

// Add integer indexed property
for (let t of ["", "abc"]) {
  for (let enumerable of [true, false]) {
    let s = new String(t);
    Object.defineProperty(s, t.length, {value: "hello", enumerable});
    let names = enumerableNames(s);

    assertEquals([...range(0, t.length + enumerable)], names);
  }
}

// Add non-integer, string valued property
for (let t of ["", "abc"]) {
  for (let enumerable of [true, false]) {
    let s = new String(t);
    Object.defineProperty(s, "p", {value: "hello", enumerable});
    let names = enumerableNames(s);

    assertEquals([...range(0, t.length), ...(enumerable ? ["p"] : [])], names);
  }
}

// Add symbol valued property
for (let t of ["", "abc"]) {
  for (let enumerable of [true, false]) {
    let s = new String(t);
    let sym = Symbol();
    Object.defineProperty(s, sym, {value: "hello", enumerable});
    let names = enumerableNames(s);

    assertEquals([...range(0, t.length)], names);
  }
}

// Add integer, string and symbol property
for (let t of ["", "abc"]) {
  for (let enumerable of [true, false]) {
    let s = new String(t);
    let sym = Symbol();
    Object.defineProperty(s, sym, {value: "hello", enumerable});
    Object.defineProperty(s, "p", {value: "hello", enumerable});
    Object.defineProperty(s, t.length, {value: "hello", enumerable});
    let names = enumerableNames(s);

    assertEquals([...range(0, t.length + enumerable), ...(enumerable ? ["p"] : [])], names);
  }
}


// Test [[Enumerate]] on String subclass
{
  let s = new (class extends String { constructor(){super("world")} })();
  let names = enumerableNames(s);

  assertEquals(["0","1","2","3","4"], names);
}
