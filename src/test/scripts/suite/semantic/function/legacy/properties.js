/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertEquals,
  assertFalse, assertTrue, assertNotNull, assertNull,
  assertNotUndefined, assertDataProperty
} = Assert;

function MakeLegacyArguments(callee, ...args) {
  var arguments = {callee, length: 0, [Symbol.iterator]: [].values};
  Object.defineProperty(arguments, "length", {enumerable: false});
  Object.defineProperty(arguments, "callee", {enumerable: false});
  Object.defineProperty(arguments, Symbol.iterator, {enumerable: false});
  Array.prototype.push.call(arguments, ...args);
  return arguments;
}

// [[GetOwnProperty]] (1)
{
  function f() { }
  assertDataProperty(f, "arguments", {value: null, writable: true, enumerable: false, configurable: false});
  assertDataProperty(f, "caller", {value: null, writable: true, enumerable: false, configurable: false});
}

// [[GetOwnProperty]] (2)
{
  (function f() {
    let desc = Object.getOwnPropertyDescriptor(f, "arguments");
    assertNotUndefined(desc);
    assertNotNull(desc.value);
    assertTrue(desc.writable);
    assertFalse(desc.enumerable);
    assertFalse(desc.configurable);
  })();
  (function f() {
    let desc = Object.getOwnPropertyDescriptor(f, "arguments");
    assertNotUndefined(desc);
    assertNotNull(desc.value);
    assertTrue(desc.writable);
    assertFalse(desc.enumerable);
    assertFalse(desc.configurable);
  })(123);
  (function f() {
    let desc = Object.getOwnPropertyDescriptor(f, "arguments");
    assertNotUndefined(desc);
    assertNotNull(desc.value);
    assertTrue(desc.writable);
    assertFalse(desc.enumerable);
    assertFalse(desc.configurable);
  })(123, 456);
  (function g() {
    (function f() {
      assertDataProperty(f, "caller", {value: g, writable: true, enumerable: false, configurable: false});
    })();
  })();
}

// [[Get]] (1)
{
  function f() { }
  assertNull(f.arguments);
  assertNull(f.caller);
}

// [[Get]] (2)
{
  (function f() {
    assertEquals(MakeLegacyArguments(f), f.arguments);
  })();
  (function f() {
    assertEquals(MakeLegacyArguments(f, 123), f.arguments);
  })(123);
  (function f() {
    assertEquals(MakeLegacyArguments(f, 123, 456), f.arguments);
  })(123, 456);
  (function g() {
    (function f() {
      assertSame(g, f.caller);
    })();
  })();
}

// [[Set]] (1)
{
  function f() { }

  assertTrue(Reflect.set(f, "arguments", 0));
  assertNull(f.arguments);

  assertTrue(Reflect.set(f, "caller", 0));
  assertNull(f.caller);
}

// [[Set]] (2)
{
  (function f() {
    var oldArguments = f.arguments;
    assertTrue(Reflect.set(f, "arguments", 0));
    assertNotSame(oldArguments, f.arguments);
  })();
  (function f() {
    var oldCaller = f.caller;
    assertTrue(Reflect.set(f, "caller", 0));
    assertSame(oldCaller, f.caller);
  })();
}

// Object.seal, Object.isSealed (arguments)
{
  function f() { return f.arguments }

  assertNotNull(f());
  assertFalse(Object.isSealed(f));
  Object.seal(f);
  assertTrue(Object.isSealed(f));
  assertNotNull(f());
}

// Object.seal, Object.isSealed (caller)
{
  function f() { return f.caller }
  function g() { return f() }

  assertSame(g, g());
  assertFalse(Object.isSealed(f));
  Object.seal(f);
  assertTrue(Object.isSealed(f));
  assertSame(g, g());
}

// Object.freeze, Object.isFrozen (arguments)
{
  function f() { return f.arguments }

  assertNotNull(f());
  assertFalse(Object.isFrozen(f));
  Object.freeze(f);
  assertTrue(Object.isFrozen(f));
  assertNull(f());
}

// Object.freeze, Object.isFrozen (caller)
{
  function f() { return f.caller }
  function g() { return f() }

  assertSame(g, g());
  assertFalse(Object.isFrozen(f));
  Object.freeze(f);
  assertTrue(Object.isFrozen(f));
  assertNull(g());
}
