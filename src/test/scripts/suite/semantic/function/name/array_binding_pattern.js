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
  // 13.2.3.5 Runtime Semantics: IteratorBindingInitialisation
  // - SingleNameBinding : BindingIdentifier Initializer
  var [f4 = function (){}] = [];
  assertFunctionName(f4, "f4");

  var [g4 = function* (){}] = [];
  assertFunctionName(g4, "g4");

  var [a4 = () => {}] = [];
  assertFunctionName(a4, "a4");

  var [c4 = class {}] = [];
  assertFunctionName(c4, "c4");

  var [d4 = class { static get name() { return "<class-name>" } }] = [];
  assertClassName(d4, "<class-name>");

  let [f5 = function (){}] = [];
  assertFunctionName(f5, "f5");

  let [g5 = function* (){}] = [];
  assertFunctionName(g5, "g5");

  let [a5 = () => {}] = [];
  assertFunctionName(a5, "a5");

  let [c5 = class {}] = [];
  assertFunctionName(c5, "c5");

  let [d5 = class { static get name() { return "<class-name>" } }] = [];
  assertClassName(d5, "<class-name>");

  const [f6 = function (){}] = [];
  assertFunctionName(f6, "f6");

  const [g6 = function* (){}] = [];
  assertFunctionName(g6, "g6");

  const [a6 = () => {}] = [];
  assertFunctionName(a6, "a6");

  const [c6 = class {}] = [];
  assertFunctionName(c6, "c6");

  const [d6 = class { static get name() { return "<class-name>" } }] = [];
  assertClassName(d6, "<class-name>");
})();
