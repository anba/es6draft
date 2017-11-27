/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// [[Enumerate]] and for-in on proxies can produce non-string keys
// https://github.com/tc39/ecma262/pull/160

function enumerateWithDelete(o) {
  var log = [];
  for (var k in o) {
    delete o.b;
    log.push(k);
  }
  return log;
}

var p1 = new Proxy({a: 0, b: 0, c: 0}, {});
assertEquals(["a", "c"], enumerateWithDelete(p1));

var p2 = new Proxy({a: 0, b: 0, c: 0}, {
  enumerate(target) {
    return Reflect.enumerate(target);
  }
});
assertEquals(["a", "c"], enumerateWithDelete(p2));
