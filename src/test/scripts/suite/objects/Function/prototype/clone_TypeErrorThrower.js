/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertNotSame, assertEquals,
  assertTrue, assertFalse,
  assertDataProperty, assertAccessorProperty,
  assertUndefined, assertCallable,
} = Assert;

// %ThrowTypeError% default properties
{
  let ThrowTypeError = Object.getOwnPropertyDescriptor(Function.prototype, "caller").get;
  let tte = Object.getOwnPropertyDescriptor(function(){"use strict"}, "caller");

  assertCallable(ThrowTypeError);
  assertUndefined(tte);
  assertUndefined(Object.getOwnPropertyDescriptor(ThrowTypeError, "arguments"));
  assertUndefined(Object.getOwnPropertyDescriptor(ThrowTypeError, "caller"));
  assertDataProperty(ThrowTypeError, "length", {value: 0, writable: false, enumerable: false, configurable: false});
  assertFalse(ThrowTypeError.hasOwnProperty("name"));
  assertEquals(["length"], Object.getOwnPropertyNames(ThrowTypeError).sort());
  assertEquals([], Object.getOwnPropertySymbols(ThrowTypeError));
  assertFalse(Object.isExtensible(ThrowTypeError));
  assertSame(Function.prototype, Object.getPrototypeOf(ThrowTypeError));
}

// Same realm clone
{
  let ThrowTypeError = Object.getOwnPropertyDescriptor(Function.prototype, "caller").get;
  let clone = ThrowTypeError.toMethod({});

  assertNotSame(ThrowTypeError, clone);
  assertUndefined(Object.getOwnPropertyDescriptor(clone, "arguments"));
  assertUndefined(Object.getOwnPropertyDescriptor(clone, "caller"));
  assertFalse(clone.hasOwnProperty("length"));
  assertFalse(clone.hasOwnProperty("name"));
  assertEquals([], Object.getOwnPropertyNames(clone).sort());
  assertEquals([], Object.getOwnPropertySymbols(clone));
  assertTrue(Object.isExtensible(clone));
  assertSame(Function.prototype, Object.getPrototypeOf(clone));
}

// Foreign realm clone
{
  let realm = new Reflect.Realm();
  let ThrowTypeError = Object.getOwnPropertyDescriptor(Function.prototype, "caller").get;
  let foreignThrowTypeError = Object.getOwnPropertyDescriptor(realm.global.Function.prototype, "caller").get;
  let clone = realm.global.Function.prototype.toMethod.call(ThrowTypeError, {});

  assertNotSame(ThrowTypeError, foreignThrowTypeError);
  assertNotSame(ThrowTypeError, clone);
  assertNotSame(foreignThrowTypeError, clone);
  assertUndefined(Object.getOwnPropertyDescriptor(clone, "arguments"));
  assertUndefined(Object.getOwnPropertyDescriptor(clone, "caller"));
  assertFalse(clone.hasOwnProperty("length"));
  assertFalse(clone.hasOwnProperty("name"));
  assertEquals([], Object.getOwnPropertyNames(clone).sort());
  assertEquals([], Object.getOwnPropertySymbols(clone));
  assertTrue(Object.isExtensible(clone));
  assertSame(Function.prototype, Object.getPrototypeOf(clone));
}
