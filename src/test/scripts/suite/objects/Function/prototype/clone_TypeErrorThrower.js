/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertNotSame, assertEquals,
  assertTrue, assertFalse,
  assertDataProperty, assertAccessorProperty,
} = Assert;

// %ThrowTypeError% default properties
{
  let ThrowTypeError = Object.getOwnPropertyDescriptor(Object, "caller").get;
  let tte = Object.getOwnPropertyDescriptor(function(){"use strict"}, "caller").get;

  assertSame(ThrowTypeError, tte);
  assertAccessorProperty(tte, "arguments", {get: ThrowTypeError, set: ThrowTypeError, enumerable: false, configurable: false});
  assertAccessorProperty(tte, "caller", {get: ThrowTypeError, set: ThrowTypeError, enumerable: false, configurable: false});
  assertDataProperty(tte, "length", {value: 0, writable: false, enumerable: false, configurable: true});
  assertFalse(tte.hasOwnProperty("name"));
  assertEquals(["arguments", "caller", "length"], Object.getOwnPropertyNames(tte).sort());
  assertEquals([], Object.getOwnPropertySymbols(tte));
  assertFalse(Object.isExtensible(tte));
  assertSame(Function.prototype, Object.getPrototypeOf(tte));
}

// Same realm clone
{
  let ThrowTypeError = Object.getOwnPropertyDescriptor(Object, "caller").get;
  let tte = Object.getOwnPropertyDescriptor(function(){"use strict"}, "caller").get;
  let clone = tte.toMethod({});

  assertNotSame(tte, clone);
  assertNotSame(ThrowTypeError, clone);
  assertAccessorProperty(clone, "arguments", {get: ThrowTypeError, set: ThrowTypeError, enumerable: false, configurable: false});
  assertAccessorProperty(clone, "caller", {get: ThrowTypeError, set: ThrowTypeError, enumerable: false, configurable: false});
  assertFalse(clone.hasOwnProperty("length"));
  assertFalse(clone.hasOwnProperty("name"));
  assertEquals(["arguments", "caller"], Object.getOwnPropertyNames(clone).sort());
  assertEquals([], Object.getOwnPropertySymbols(clone));
  assertTrue(Object.isExtensible(clone));
  assertSame(Function.prototype, Object.getPrototypeOf(clone));
}

// Foreign realm clone
{
  let realm = new Reflect.Realm();
  let ThrowTypeError = Object.getOwnPropertyDescriptor(Object, "caller").get;
  let foreignThrowTypeError = Object.getOwnPropertyDescriptor(realm.global.Object, "caller").get;
  let tte = Object.getOwnPropertyDescriptor(function(){"use strict"}, "caller").get;
  let clone = realm.global.Function.prototype.toMethod.call(tte, {});

  assertNotSame(ThrowTypeError, foreignThrowTypeError);
  assertNotSame(tte, clone);
  assertNotSame(ThrowTypeError, clone);
  assertNotSame(foreignThrowTypeError, clone);
  assertAccessorProperty(clone, "arguments", {get: foreignThrowTypeError, set: foreignThrowTypeError, enumerable: false, configurable: false});
  assertAccessorProperty(clone, "caller", {get: foreignThrowTypeError, set: foreignThrowTypeError, enumerable: false, configurable: false});
  assertFalse(clone.hasOwnProperty("length"));
  assertFalse(clone.hasOwnProperty("name"));
  assertEquals(["arguments", "caller"], Object.getOwnPropertyNames(clone).sort());
  assertEquals([], Object.getOwnPropertySymbols(clone));
  assertTrue(Object.isExtensible(clone));
  assertSame(Function.prototype, Object.getPrototypeOf(clone));
}
