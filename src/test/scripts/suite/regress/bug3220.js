/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertEquals, assertThrows, fail
} = Assert;

// Set/Map/WeakSet/WeakMap needs to call IteratorClose
// https://bugs.ecmascript.org/show_bug.cgi?id=3220

class CollectionError extends Error {
}

class ThrowMap extends Map {
  set(k, v) { throw new CollectionError }
}

class ThrowSet extends Set {
  add(v) { throw new CollectionError }
}

class ThrowWeakMap extends WeakMap {
  set(k, v) { throw new CollectionError }
}

class ThrowWeakSet extends WeakSet {
  add(v) { throw new CollectionError }
}

let log;
function makeSource(v) {
  return function*(){
    try {
      log.push("yield");
      yield v;
      fail `unexpected after yield`;
    } finally {
      log.push("finally");
    }
    fail `unexpected after finally`;
  }();
}

log = [];
assertThrows(CollectionError, () => new ThrowMap(makeSource([])));
assertEquals(["yield"], log);

log = [];
assertThrows(CollectionError, () => new ThrowSet(makeSource([])));
assertEquals(["yield"], log);

log = [];
assertThrows(CollectionError, () => new ThrowWeakMap(makeSource([])));
assertEquals(["yield"], log);

log = [];
assertThrows(CollectionError, () => new ThrowWeakSet(makeSource([])));
assertEquals(["yield"], log);
