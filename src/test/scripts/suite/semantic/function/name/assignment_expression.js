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
{
  let f0, g0, a0, c0, d0;

  // 12.13.4 Runtime Semantics: Evaluation
  // - AssignmentExpression[in, yield] : LeftHandSideExpression[?yield] = AssignmentExpression[?in, ?yield]
  // where LeftHandSideExpression is IdentifierReference
  f0 = function() {};
  assertFunctionName(f0, "f0");

  g0 = function*() {};
  assertFunctionName(g0, "g0");

  a0 = () => {};
  assertFunctionName(a0, "a0");

  c0 = class {};
  assertFunctionName(c0, "c0");

  d0 = class { static get name() { return "<class-name>" } };
  assertClassName(d0, "<class-name>");
}
