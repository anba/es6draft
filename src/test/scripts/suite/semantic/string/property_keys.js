/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame,
  assertEquals,
} = Assert;

function assertArrayEquals(expected, actual) {
  return assertEquals(expected.sort(), actual.sort());
}

// [[Enumerate]] and [[OwnPropertyKeys]] need to handle indexed property keys created before initialisation
{
  let s = new (class extends String { constructor(){} })();
  Object.defineProperty(s, "0", {value: "hello", enumerable: true});
  String.call(s, "world");
  let names = [...{[Symbol.iterator]: () => Reflect.enumerate(s)}];
  let ownNames = Object.getOwnPropertyNames(s);

  assertSame("hello", s[0]);
  assertSame(5, names.length);
  assertSame(5 + 1, ownNames.length);
  assertArrayEquals(["0","1","2","3","4"], names);
  assertArrayEquals(["0","length","1","2","3","4"], ownNames);
}

// [[Enumerate]] and [[OwnPropertyKeys]] need to handle indexed property keys created before initialisation
{
  let s = new (class extends String { constructor(){} })();
  Object.defineProperty(s, "0", {value: "hello", enumerable: false});
  String.call(s, "world");
  let names = [...{[Symbol.iterator]: () => Reflect.enumerate(s)}];
  let ownNames = Object.getOwnPropertyNames(s);

  assertSame("hello", s[0]);
  assertSame(4, names.length);
  assertSame(5 + 1, ownNames.length);
  assertArrayEquals(["1","2","3","4"], names);
  assertArrayEquals(["0","length","1","2","3","4"], ownNames);
}
