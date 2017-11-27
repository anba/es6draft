/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue, assertFalse
} = Assert;

// 9.3.3: Remove step 3 from CreateOwnDataProperty
// https://bugs.ecmascript.org/show_bug.cgi?id=1549

{
  let object = {};
  let receiver = {foo: 0};
  let success = Reflect.set(object, "foo", 1, receiver);
  assertTrue(success);
  assertFalse(object.hasOwnProperty("foo"));
  assertTrue(receiver.hasOwnProperty("foo"));
  assertSame(1, receiver.foo);
}

{
  let object = {};
  let receiver = {};

  let success = Reflect.set(object, "foo", 1, receiver);
  assertTrue(success);
  assertFalse(object.hasOwnProperty("foo"));
  assertTrue(receiver.hasOwnProperty("foo"));
  assertSame(1, receiver.foo);
}

{
  let object = {};
  let receiver = {};
  Object.defineProperty(receiver, "foo", {value: 0, writable: true, enumerable: false, configurable: false});

  let success = Reflect.set(object, "foo", 1, receiver);
  assertTrue(success);
  assertFalse(object.hasOwnProperty("foo"));
  assertTrue(receiver.hasOwnProperty("foo"));
  assertSame(1, receiver.foo);
  assertFalse(receiver.propertyIsEnumerable("foo"));
}
