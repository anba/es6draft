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
  // 12.13.5.3 Runtime Semantics: IteratorDestructuringAssignmentEvaluation
  // - AssignmentElement[Yield] : DestructuringAssignmentTarget Initializer{opt}
  var f4, g4, a4, c4, d4;

  [f4 = function (){}] = [];
  assertFunctionName(f4, "f4");

  [g4 = function* (){}] = [];
  assertFunctionName(g4, "g4");

  [a4 = () => {}] = [];
  assertFunctionName(a4, "a4");

  [c4 = class {}] = [];
  assertFunctionName(c4, "c4");

  [d4 = class { static get name() { return "<class-name>" } }] = [];
  assertClassName(d4, "<class-name>");
})();
