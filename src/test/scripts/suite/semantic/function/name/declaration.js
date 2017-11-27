/*
 * Copyright (c) André Bargull
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

// function/generator/class declaration
(function() {
  function f() {}
  assertFunctionName(f, "f");

  function* g() {}
  assertFunctionName(g, "g");

  class ClassWithoutName {}
  assertFunctionName(ClassWithoutName, "ClassWithoutName");

  class ClassWithName { static get name() { return "OwnName" } }
  assertClassName(ClassWithName, "OwnName");

  class ClassWithComputedName { static get ["name"]() { return "OwnName" } }
  assertClassName(ClassWithComputedName, "OwnName");
})();
