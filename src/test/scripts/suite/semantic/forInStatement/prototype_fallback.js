/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertTrue, assertFalse, assertEquals
} = Assert;

function propertyDeletedFallbackFromPrototype() {
  var p = {a: 0, b: 0, c: 0, d: 0};
  var o = {__proto__: p, a: 1, b: 1, c: 1};
  var visited = [];
  for (var k in o) {
    visited.push(k);
    if (k == "a") delete o.b;
  }
  assertEquals(["a", "c", "b", "d"], visited);
}
propertyDeletedFallbackFromPrototype();

function propertyDeletedNoFallbackFromPrototypeNotPresent() {
  var p = {a: 0, c: 0, d: 0};
  var o = {__proto__: p, a: 1, b: 1, c: 1};
  var visited = [];
  for (var k in o) {
    visited.push(k);
    if (k == "a") delete o.b;
  }
  assertEquals(["a", "c", "d"], visited);
}
propertyDeletedNoFallbackFromPrototypeNotPresent();

function propertyDeletedNoFallbackFromPrototypeNonEnumerable() {
  var p = Object.defineProperty({a: 0, b: 0, c: 0, d: 0}, "b", {enumerable: false});
  var o = {__proto__: p, a: 1, b: 1, c: 1};
  var visited = [];
  for (var k in o) {
    visited.push(k);
    if (k == "a") delete o.b;
  }
  assertEquals(["a", "c", "d"], visited);
}
propertyDeletedNoFallbackFromPrototypeNonEnumerable();
