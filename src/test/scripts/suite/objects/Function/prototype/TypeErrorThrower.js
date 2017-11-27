/*
 * Copyright (c) André Bargull
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
