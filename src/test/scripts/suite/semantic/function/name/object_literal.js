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

// TODO: __proto__ and .name

// anonymous function/generator/arrow/class expression in object literal
{
  const symbolWithoutDescription = Symbol();
  const symbolWithDescription = Symbol("desc");

  // 12.1.5.8 Runtime Semantics: PropertyDefinitionEvaluation
  // - PropertyDefinition : PropertyName : AssignmentExpression
  let f0 = {f: function() {}}.f;
  assertFunctionName(f0, "f");

  let g0 = {g: function*() {}}.g;
  assertFunctionName(g0, "g");

  let a0 = {a: () => {}}.a;
  assertFunctionName(a0, "a");

  let c0 = {c: class {}}.c;
  assertFunctionName(c0, "c");

  let d0 = {d: class { static get name() { return "<class-name>" } }}.d;
  assertClassName(d0, "<class-name>");

  let f1 = {["f"]: function() {}}.f;
  assertFunctionName(f1, "f");

  let g1 = {["g"]: function*() {}}.g;
  assertFunctionName(g1, "g");

  let a1 = {["a"]: () => {}}.a;
  assertFunctionName(a1, "a");

  let c1 = {["c"]: class {}}.c;
  assertFunctionName(c1, "c");

  let d1 = {["d"]: class { static get name() { return "<class-name>" } }}.d;
  assertClassName(d1, "<class-name>");

  let f2 = {[symbolWithoutDescription]: function() {}}[symbolWithoutDescription];
  assertFunctionName(f2, "");

  let g2 = {[symbolWithoutDescription]: function*() {}}[symbolWithoutDescription];
  assertFunctionName(g2, "");

  let a2 = {[symbolWithoutDescription]: () => {}}[symbolWithoutDescription];
  assertFunctionName(a2, "");

  let c2 = {[symbolWithoutDescription]: class {}}[symbolWithoutDescription];
  assertFunctionName(c2, "");

  let d2 = {[symbolWithoutDescription]: class { static get name() { return "<class-name>" } }}[symbolWithoutDescription];
  assertClassName(d2, "<class-name>");

  let f3 = {[symbolWithDescription]: function() {}}[symbolWithDescription];
  assertFunctionName(f3, "[desc]");

  let g3 = {[symbolWithDescription]: function*() {}}[symbolWithDescription];
  assertFunctionName(g3, "[desc]");

  let a3 = {[symbolWithDescription]: () => {}}[symbolWithDescription];
  assertFunctionName(a3, "[desc]");

  let c3 = {[symbolWithDescription]: class {}}[symbolWithDescription];
  assertFunctionName(c3, "[desc]");

  let d3 = {[symbolWithDescription]: class { static get name() { return "<class-name>" } }}[symbolWithDescription];
  assertClassName(d3, "<class-name>");
}
