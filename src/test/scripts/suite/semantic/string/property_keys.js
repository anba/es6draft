/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
  assertEquals,
} = Assert;

// Test [[Enumerate]] and [[OwnPropertyKeys]] on String subclass
{
  let string = new (class extends String { constructor(s){super(s)} })("world");
  let names = [...Reflect.enumerate(string)];
  let ownNames = Object.getOwnPropertyNames(string);

  assertSame(5, names.length);
  assertSame(5 + 1, ownNames.length);
  assertEquals(["0","1","2","3","4"], names);
  assertEquals(["0","1","2","3","4","length"], ownNames);
}
