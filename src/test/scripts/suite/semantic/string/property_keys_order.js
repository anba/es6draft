/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
  assertEquals,
} = Assert;

function enumerableNames(o) {
  return [...function*(){ for (let k in o) yield k; }()];
}

function getOwnKeys(o) {
  return Reflect.ownKeys(Object(o));
}

function* range(start, end) {
  for (; start < end; ++start) {
    yield "" + start;
  }
}

// [[Enumerate]] and [[OwnPropertyKeys]] report integer indexed, followed by string and finally symbol properties
for (let s of ["", new String("")]) {
  let names = enumerableNames(s);
  let ownNames = Object.getOwnPropertyNames(s);
  let ownSymbols = Object.getOwnPropertySymbols(s);
  let ownKeys = getOwnKeys(s);

  assertEquals([], names);
  assertEquals(["length"], ownNames);
  assertEquals([], ownSymbols);
  assertEquals(["length"], ownKeys);
}

for (let s of ["abc", new String("abc")]) {
  let names = enumerableNames(s);
  let ownNames = Object.getOwnPropertyNames(s);
  let ownSymbols = Object.getOwnPropertySymbols(s);
  let ownKeys = getOwnKeys(s);

  assertEquals(["0","1","2"], names);
  assertEquals(["0","1","2","length"], ownNames);
  assertEquals([], ownSymbols);
  assertEquals(["0","1","2","length"], ownKeys);
}

// Add integer indexed property
for (let t of ["", "abc"]) {
  for (let enumerable of [true, false]) {
    let s = new String(t);
    Object.defineProperty(s, t.length, {value: "hello", enumerable});
    let names = enumerableNames(s);
    let ownNames = Object.getOwnPropertyNames(s);
    let ownSymbols = Object.getOwnPropertySymbols(s);
    let ownKeys = getOwnKeys(s);

    assertEquals([...range(0, t.length + enumerable)], names);
    assertEquals([...range(0, t.length + 1), "length"], ownNames);
    assertEquals([], ownSymbols);
    assertEquals([...range(0, t.length + 1), "length"], ownKeys);
  }
}

// Add non-integer, string valued property
for (let t of ["", "abc"]) {
  for (let enumerable of [true, false]) {
    let s = new String(t);
    Object.defineProperty(s, "p", {value: "hello", enumerable});
    let names = enumerableNames(s);
    let ownNames = Object.getOwnPropertyNames(s);
    let ownSymbols = Object.getOwnPropertySymbols(s);
    let ownKeys = getOwnKeys(s);

    assertEquals([...range(0, t.length), ...(enumerable ? ["p"] : [])], names);
    assertEquals([...range(0, t.length), "length", "p"], ownNames);
    assertEquals([], ownSymbols);
    assertEquals([...range(0, t.length), "length", "p"], ownKeys);
  }
}

// Add symbol valued property
for (let t of ["", "abc"]) {
  for (let enumerable of [true, false]) {
    let s = new String(t);
    let sym = Symbol();
    Object.defineProperty(s, sym, {value: "hello", enumerable});
    let names = enumerableNames(s);
    let ownNames = Object.getOwnPropertyNames(s);
    let ownSymbols = Object.getOwnPropertySymbols(s);
    let ownKeys = getOwnKeys(s);

    assertEquals([...range(0, t.length)], names);
    assertEquals([...range(0, t.length), "length"], ownNames);
    assertEquals([sym], ownSymbols);
    assertEquals([...range(0, t.length), "length", sym], ownKeys);
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
    let ownNames = Object.getOwnPropertyNames(s);
    let ownSymbols = Object.getOwnPropertySymbols(s);
    let ownKeys = getOwnKeys(s);

    assertEquals([...range(0, t.length + enumerable), ...(enumerable ? ["p"] : [])], names);
    assertEquals([...range(0, t.length + 1), "length", "p"], ownNames);
    assertEquals([sym], ownSymbols);
    assertEquals([...range(0, t.length + 1), "length", "p", sym], ownKeys);
  }
}


// Test [[Enumerate]] and [[OwnPropertyKeys]] on String subclass
{
  let s = new (class extends String { constructor(){super("world")} })();
  let names = enumerableNames(s);
  let ownNames = Object.getOwnPropertyNames(s);
  let ownKeys = getOwnKeys(s);

  assertEquals(["0","1","2","3","4"], names);
  assertEquals(["0","1","2","3","4","length"], ownNames);
  assertEquals(["0","1","2","3","4","length"], ownKeys);
}
