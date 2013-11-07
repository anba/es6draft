/*
 * Copyright (c) 2012-2013 André Bargull
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
  // 13.2.2.3 Runtime Semantics: Evaluation
  // - VariableDeclaration : BindingIdentifier Initialiser
  var f1 = function (){};
  assertFunctionName(f1, "f1");

  var g1 = function* (){};
  assertFunctionName(g1, "g1");

  var a1 = () => {};
  assertFunctionName(a1, "a1");

  var c1 = class {};
  assertFunctionName(c1, "c1");

  var d1 = class { static get name() { return "<class-name>" } };
  assertClassName(d1, "<class-name>");

  // 13.2.1.6 Runtime Semantics: Evaluation
  // - LexicalBinding : BindingIdentifier Initialiser
  let f2 = function (){};
  assertFunctionName(f2, "f2");

  let g2 = function* (){};
  assertFunctionName(g2, "g2");

  let a2 = () => {};
  assertFunctionName(a2, "a2");

  let c2 = class {};
  assertFunctionName(c2, "c2");

  let d2 = class { static get name() { return "<class-name>" } };
  assertClassName(d2, "<class-name>");

  const f3 = function (){};
  assertFunctionName(f3, "f3");

  const g3 = function* (){};
  assertFunctionName(g3, "g3");

  const a3 = () => {};
  assertFunctionName(a3, "a3");

  const c3 = class {};
  assertFunctionName(c3, "c3");

  const d3 = class { static get name() { return "<class-name>" } };
  assertClassName(d3, "<class-name>");
})();
