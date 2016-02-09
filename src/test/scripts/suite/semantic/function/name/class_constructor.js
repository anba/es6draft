/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue, assertFalse, assertDataProperty
} = Assert;

function assertFunctionName(f, name) {
  return assertDataProperty(f, "name", {value: name, writable: false, enumerable: false, configurable: true});
}

function assertMethodName(o, pk, name) {
  let desc = Object.getOwnPropertyDescriptor(o, pk);
  let m = 'value' in desc ? desc.value : desc.get !== void 0 ? desc.get : desc.set;
  return assertFunctionName(m, name);
}

function assertClassName(c, name) {
  assertTrue(c.hasOwnProperty("name"));
  return assertSame(name, c.name);
}

function assertAnonymousFunction(f) {
  return assertFalse(f.hasOwnProperty("name"));
}

// constructor() in class (prototype)
{
  class C { constructor() {} }
  let c1 = class c { constructor() {} };
  let c2 = class { constructor() {} };
  let c3 = (1, class { constructor() {} });

  assertClassName(C, "C");
  assertClassName(c1, "c");
  assertClassName(c2, "c2");
  assertAnonymousFunction(c3);
}

// constructor() in class (static)
{
  class C { static constructor() {} }
  let c1 = class c { static constructor() {} };
  let c2 = class { static constructor() {} };
  let c3 = (1, class { static constructor() {} });

  // assert that "constructor" as a static method is not special-cased
  assertMethodName(C, "constructor", "constructor");
  assertMethodName(c1, "constructor", "constructor");
  assertMethodName(c2, "constructor", "constructor");
  assertMethodName(c3, "constructor", "constructor");
}
