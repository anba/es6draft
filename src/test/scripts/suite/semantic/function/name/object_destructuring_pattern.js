/*
 * Copyright (c) 2012-2015 André Bargull
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


// anonymous function/generator/arrow/class expression
(function() {
  // 12.13.5.4 Runtime Semantics: KeyedDestructuringAssignmentEvaluation
  // - AssignmentElement[Yield] : DestructuringAssignmentTarget Initializer{opt}
  var f7, g7, a7, c7, d7;
  var f10, g10, a10, c10, d10;
  var f13, g13, a13, c13, d13;

  ({f7 = function (){}}) = [];
  assertFunctionName(f7, "f7");

  ({g7 = function* (){}}) = [];
  assertFunctionName(g7, "g7");

  ({a7 = () => {}}) = [];
  assertFunctionName(a7, "a7");

  ({c7 = class {}}) = [];
  assertFunctionName(c7, "c7");

  ({d7 = class { static get name() { return "<class-name>" } }}) = [];
  assertClassName(d7, "<class-name>");

  ({key: f10 = function (){}}) = [];
  assertFunctionName(f10, "f10");

  ({key: g10 = function* (){}}) = [];
  assertFunctionName(g10, "g10");

  ({key: a10 = () => {}}) = [];
  assertFunctionName(a10, "a10");

  ({key: c10 = class {}}) = [];
  assertFunctionName(c10, "c10");

  ({key: d10 = class { static get name() { return "<class-name>" } }}) = [];
  assertClassName(d10, "<class-name>");

  ({["key"]: f13 = function (){}}) = [];
  assertFunctionName(f13, "f13");

  ({["key"]: g13 = function* (){}}) = [];
  assertFunctionName(g13, "g13");

  ({["key"]: a13 = () => {}}) = [];
  assertFunctionName(a13, "a13");

  ({["key"]: c13 = class {}}) = [];
  assertFunctionName(c13, "c13");

  ({["key"]: d13 = class { static get name() { return "<class-name>" } }}) = [];
  assertClassName(d13, "<class-name>");
})();
