/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
  assertTrue,
  assertEquals,
} = Assert;

// add entry, then keys()
{
  let map = new Map();
  map.set(0, 0);
  let keys = map.keys();
  assertEquals({value: 0, done: false}, keys.next());
  assertEquals({value: void 0, done: true}, keys.next());
}

// keys(), then add entry
{
  let map = new Map();
  let keys = map.keys();
  map.set(0, 0);
  assertEquals({value: 0, done: false}, keys.next());
  assertEquals({value: void 0, done: true}, keys.next());
}

// keys(), then add entries
{
  let map = new Map();
  let keys = map.keys();
  map.set(0, 0);
  assertEquals({value: 0, done: false}, keys.next());
  map.set(1, 1);
  assertEquals({value: 1, done: false}, keys.next());
  assertEquals({value: void 0, done: true}, keys.next());
}

// keys(), then clear and add entry
{
  let map = new Map();
  let keys = map.keys();
  map.set(0, 0);
  assertEquals({value: 0, done: false}, keys.next());
  map.clear();
  map.set(1, 1);
  assertEquals({value: 1, done: false}, keys.next());
  assertEquals({value: void 0, done: true}, keys.next());
}
