/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue, assertEquals, assertUndefined, assertSyntaxError,
} = Assert;

function CreateDecorator() {
  "use strict";
  let called = false, parameters;
  return {
    decorator(...rest) {
      assertUndefined(this);
      assertFalse(called);
      called = true;
      parameters = rest;
    },
    called() {
      return called;
    },
    parameters() {
      return parameters;
    },
  };
}

function ClassMethodDesc(m) {
  return {value: m, writable: true, enumerable: false, configurable: true};
}

function ObjectMethodDesc(m) {
  return {value: m, writable: true, enumerable: true, configurable: true};
}

// Decorated class declaration at statement level
assertSyntaxError(`if (0) @decorator class C {}`);

// ClassDeclaration
{
  let {decorator, called, parameters} = CreateDecorator();
  @decorator class Decl { }
  assertTrue(called());
  assertEquals([Decl], parameters());
}

// ClassExpression
{
  let {decorator, called, parameters} = CreateDecorator();
  let clazz = @decorator class { };
  assertTrue(called());
  assertEquals([clazz], parameters());
}

// Named ClassExpression
{
  let {decorator, called, parameters} = CreateDecorator();
  let clazz = @decorator class Expr { };
  assertTrue(called());
  assertEquals([clazz], parameters());
}

// Class methods
{
  let {decorator, called, parameters} = CreateDecorator();
  class C { @decorator m() {} }
  assertTrue(called());
  assertEquals([C.prototype, "m", ClassMethodDesc(C.prototype.m)], parameters());
}

// Class methods (static)
{
  let {decorator, called, parameters} = CreateDecorator();
  class C { @decorator static m() {} }
  assertTrue(called());
  assertEquals([C, "m", ClassMethodDesc(C.m)], parameters());
}

// Object literal methods
{
  let {decorator, called, parameters} = CreateDecorator();
  let obj = { @decorator m() {} };
  assertTrue(called());
  assertEquals([obj, "m", ObjectMethodDesc(obj.m)], parameters());
}

// Computed property names
{
  let decorator = () => {};
  ({ @decorator [Symbol.iterator] () {} });
  class C { @decorator [Symbol.iterator] () {} };
  (class { @decorator [Symbol.iterator] () {} });
}
