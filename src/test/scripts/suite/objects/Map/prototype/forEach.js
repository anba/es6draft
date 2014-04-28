/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame,
  assertThrows,
  assertTrue,
} = Assert;


// New entries during forEach iteration
{
  let map = new Map();
  let key = 0;
  map.set(0, 0);
  map.forEach(function(k, v) {
    assertSame(k, v);
    assertSame(key, k);
    if (++key < 5) {
      map.set(key, key);
    }
  });
  assertSame(5, key);
}

// clear() during forEach iteration
{
  let map = new Map();
  let called = 0;
  map.set(0, 0);
  map.set(1, 1);
  map.forEach(function(k, v) {
    assertSame(k, v);
    assertSame(0, k);
    assertSame(0, called++);
    map.clear();
  });
  assertSame(1, called);
}
