/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertTrue, assertDataProperty
} = Assert;

function assertFunctionName(f, name) {
  return assertDataProperty(f, "name", {value: name, writable: false, enumerable: false, configurable: true});
}

function assertClassName(c, name) {
  assertTrue(c.hasOwnProperty("name"));
  return assertSame(name, c.name);
}

// named function/generator/class expression
(function() {
  var f0, g0, c0, d0;

  f0 = function f(){};
  assertFunctionName(f0, "f");

  g0 = function* g(){};
  assertFunctionName(g0, "g");

  c0 = class c{};
  assertFunctionName(c0, "c");

  d0 = class d{ static get name() { return "<class-name>" } };
  assertClassName(d0, "<class-name>");

  var f1 = function f(){};
  assertFunctionName(f1, "f");

  var g1 = function* g(){};
  assertFunctionName(g1, "g");

  var c1 = class c{};
  assertFunctionName(c1, "c");

  var d1 = class d{ static get name() { return "<class-name>" } };
  assertClassName(d1, "<class-name>");

  let f2 = function f(){};
  assertFunctionName(f2, "f");

  let g2 = function* g(){};
  assertFunctionName(g2, "g");

  let c2 = class c{};
  assertFunctionName(c2, "c");

  let d2 = class d{ static get name() { return "<class-name>" } };
  assertClassName(d2, "<class-name>");

  const f3 = function f(){};
  assertFunctionName(f3, "f");

  const g3 = function* g(){};
  assertFunctionName(g3, "g");

  const c3 = class c{};
  assertFunctionName(c3, "c");

  const d3 = class d{ static get name() { return "<class-name>" } };
  assertClassName(d3, "<class-name>");
})();
