/*
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

(function Assert(global) {
"use strict";

const {
  Object, Function, Proxy, String, SyntaxError
} = global;

const {
  getOwnPropertyDescriptor: Object_getOwnPropertyDescriptor,
  getPrototypeOf: Object_getPrototypeOf,
  is: Object_is,
} = Object;

const {
  toString: Object_prototype_toString,
} = Object.prototype;

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);

const ThrowTypeError = Object.getOwnPropertyDescriptor(function(){"use strict"}, "caller").get;

function IsCallable(o) {
  try{
    (new Proxy(o, {apply: () => {}}))();
    return true;
  } catch(e) {
    return false;
  }
}

function IsConstructor(o) {
  try{
    new (new Proxy(o, {construct: () => ({})}));
    return true;
  } catch(e) {
    return false;
  }
}

function PropertyKeyToString(pk) {
  switch(typeof pk) {
    case 'symbol':
      return String(Object(pk));
    case 'string':
    default:
      return String(pk);
  }
}

function safeToString(o) {
  try {
    return String(o);
  } catch (e) {
    try {
      return $CallFunction(Object_prototype_toString, o);
    } catch (e) {
      return "???";
    }
  }
}

function fmt(callSite, ...substitutions) {
  let cooked = Object(callSite);
  let literalSegments = cooked.length >>> 0;
  if (literalSegments === 0) {
    return "";
  }
  let r = "";
  for (let nextIndex = 0;; ++nextIndex) {
    r += cooked[nextIndex];
    if (nextIndex + 1 === literalSegments) {
      return r;
    }
    r += safeToString(substitutions[nextIndex]);
  }
}

function label(s) {
  return s !== "" ? s + ": " : "";
}

class AssertionError extends Error {
  get name() {
    return "AssertionError";
  }
}

function fail(format = "", ...args) {
  let message = typeof format === "string" ? format : fmt(format, ...args);
  throw new AssertionError(message);
}

function assertSame(expected, actual, message = "") {
  if (!Object_is(expected, actual)) {
    fail `${label(message)}Expected «${expected}», but got «${actual}»`;
  }
}

function assertNotSame(expected, actual, message = "") {
  if (Object_is(expected, actual)) {
    fail `${label(message)}Expected not «${expected}»`;
  }
}

function assertThrows(f, expected, message = "") {
  try {
    f();
  } catch (e) {
    if (e instanceof expected) {
      return;
    }
    fail `${label(message)}Expected error «${expected.name}», but got «${e}»`;
  }
  fail `${label(message)}Expected error «${expected.name}»`;
}

function assertSyntaxError(code, message = "") {
  return assertThrows(() => Function(code), SyntaxError, message);
}

function assertTrue(actual, message = "") {
  return assertSame(true, actual, message);
}

function assertFalse(actual, message = "") {
  return assertSame(false, actual, message);
}

function assertUndefined(actual, message = "") {
  return assertSame(void 0, actual, message);
}

function assertNotUndefined(actual, message = "") {
  return assertNotSame(void 0, actual, message);
}

function assertNull(actual, message = "") {
  return assertSame(null, actual, message);
}

function assertNotNull(actual, message = "") {
  return assertNotSame(null, actual, message);
}

function assertInstanceOf(constructor, value, message = `Is an instance of ${constructor.name}`) {
  return assertTrue(value instanceof constructor, message);
}

function assertCallable(o, message = "Is a [[Callable]] object") {
  return assertTrue(IsCallable(o), message);
}

function assertNotCallable(o, message = "Is not a [[Callable]] object") {
  return assertFalse(IsCallable(o), message);
}

function assertConstructor(o, message = "Is a [[Constructor]] object") {
  return assertTrue(IsConstructor(o), message);
}

function assertNotConstructor(o, message = "Is not a [[Constructor]] object") {
  return assertFalse(IsConstructor(o), message);
}

function assertDataProperty(object, propertyKey, {value, writable, enumerable, configurable}) {
  let desc = Object_getOwnPropertyDescriptor(object, propertyKey);
  assertNotUndefined(desc, `${PropertyKeyToString(propertyKey)} not found`);
  assertTrue('value' in desc);
  assertSame(value, desc.value, `${PropertyKeyToString(propertyKey)}.[[Value]]`);
  assertSame(writable, desc.writable, `${PropertyKeyToString(propertyKey)}.[[Writable]]`);
  assertSame(enumerable, desc.enumerable, `${PropertyKeyToString(propertyKey)}.[[Enumerable]]`);
  assertSame(configurable, desc.configurable, `${PropertyKeyToString(propertyKey)}.[[Configurable]]`);
}

function assertAccessorProperty(object, propertyKey, {get, set, enumerable, configurable}) {
  let desc = Object_getOwnPropertyDescriptor(object, propertyKey);
  assertNotUndefined(desc, `${PropertyKeyToString(propertyKey)} not found`);
  assertTrue('get' in desc);
  assertSame(get, desc.get, `${PropertyKeyToString(propertyKey)}.[[Get]]`);
  assertSame(set, desc.set, `${PropertyKeyToString(propertyKey)}.[[Set]]`);
  assertSame(enumerable, desc.enumerable, `${PropertyKeyToString(propertyKey)}.[[Enumerable]]`);
  assertSame(configurable, desc.configurable, `${PropertyKeyToString(propertyKey)}.[[Configurable]]`);
}

function assertBuiltinFunction(fun, name, arity) {
  assertSame("function", typeof fun);
  assertCallable(fun);
  assertNotConstructor(fun);
  assertSame(Function.prototype, Object_getPrototypeOf(fun), `${name}.[[Prototype]]`);
  assertDataProperty(fun, "length", {value: arity, writable: false, enumerable: false, configurable: true});
  assertDataProperty(fun, "name", {value: name, writable: false, enumerable: false, configurable: false});
  assertAccessorProperty(fun, "arguments", {get: ThrowTypeError, set: ThrowTypeError, enumerable: false, configurable: false});
  assertAccessorProperty(fun, "caller", {get: ThrowTypeError, set: ThrowTypeError, enumerable: false, configurable: false});
}

function assertBuiltinConstructor(fun, name, arity) {
  assertSame("function", typeof fun);
  assertCallable(fun);
  assertConstructor(fun);
  assertSame(Function.prototype, Object_getPrototypeOf(fun), `${name}.[[Prototype]]`);
  assertDataProperty(fun, "length", {value: arity, writable: false, enumerable: false, configurable: true});
  assertDataProperty(fun, "name", {value: name, writable: false, enumerable: false, configurable: false});
  assertAccessorProperty(fun, "arguments", {get: ThrowTypeError, set: ThrowTypeError, enumerable: false, configurable: false});
  assertAccessorProperty(fun, "caller", {get: ThrowTypeError, set: ThrowTypeError, enumerable: false, configurable: false});
}

function assertBuiltinPrototype(o, proto = Object.prototype) {
  if (o === Function.prototype) {
    assertSame("function", typeof o);
    assertCallable(o);
  } else {
    assertSame("object", typeof o);
    assertNotCallable(o);
  }
  assertNotConstructor(o);
  assertSame(proto, Object_getPrototypeOf(o), `[[Prototype]]`);
}

// export...
Object.defineProperty(global, "Assert", {value: {
  AssertionError, fail,
  assertSame, assertNotSame,
  assertThrows, assertSyntaxError,
  assertTrue, assertFalse,
  assertUndefined, assertNotUndefined,
  assertNull, assertNotNull,
  assertInstanceOf,
  assertCallable, assertNotCallable,
  assertConstructor, assertNotConstructor,
  assertDataProperty, assertAccessorProperty,
  assertBuiltinFunction, assertBuiltinConstructor, assertBuiltinPrototype,
}});

})(this);
