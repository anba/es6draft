/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const Assert = function(global) {
"use strict";

const {
  Object, Function, Proxy, Reflect, Set, String, Symbol, SyntaxError
} = global;

const {
  getOwnPropertyDescriptor: Reflect_getOwnPropertyDescriptor,
  getPrototypeOf: Reflect_getPrototypeOf,
  ownKeys: Reflect_ownKeys,
} = Reflect;

const {
  create: Object_create,
  getOwnPropertyDescriptor: Object_getOwnPropertyDescriptor,
  is: Object_is,
} = Object;

const {
  hasOwnProperty: Object_prototype_hasOwnProperty,
  toString: Object_prototype_toString,
} = Object.prototype;

const {
  toString: Function_prototype_toString,
} = Function.prototype;

const {
  includes: String_prototype_includes,
} = String.prototype;

const {
  add: Set_prototype_add,
  delete: Set_prototype_delete,
  forEach: Set_prototype_forEach,
} = Set.prototype;

const Set_prototype_size = Reflect_getOwnPropertyDescriptor(Set.prototype, "size").get;

const ThrowTypeError = Reflect_getOwnPropertyDescriptor(Function.prototype, "caller").get;

const $CallFunction = Function.prototype.call.bind(Function.prototype.call);

function IsCallable(o) {
  try {
    (new Proxy(o, {apply: () => {}}))();
    return true;
  } catch (e) {
    return false;
  }
}

function IsConstructor(o) {
  try {
    new (new Proxy(o, {construct: () => ({})}));
    return true;
  } catch (e) {
    return false;
  }
}

function IsPrimitive(v) {
  switch (typeof v) {
    case "undefined":
    case "boolean":
    case "number":
    case "string":
    case "symbol":
      return true;
    case "object":
      return v === null;
    default:
      return false;
  }
}

function PropertyKeyToString(propertyKey) {
  return String(propertyKey);
}

function IsDataPropertyDescriptor(desc) {
  return $CallFunction(Object_prototype_hasOwnProperty, desc, "value");
}

class PropertyKeySet extends null {
  constructor(keys) {
    var obj = Object_create(new.target.prototype);
    obj._set = new Set();
    for (let i = 0, len = keys.length; i < len; ++i) {
      obj.add(keys[i]);
    }
    return obj;
  }

  add(value) {
    return $CallFunction(Set_prototype_add, this._set, value);
  }

  delete(value) {
    return $CallFunction(Set_prototype_delete, this._set, value);
  }

  forEach(callback) {
    return $CallFunction(Set_prototype_forEach, this._set, callback);
  }

  get size() {
    return $CallFunction(Set_prototype_size, this._set);
  }

  toString() {
    let s = "";
    let n = this.size;
    this.forEach(value => {
      s += PropertyKeyToString(value);
      if (--n) {
        s += ", ";
      }
    });
    return `[${s}]`;
  }
}

function safeToString(v) {
  switch (typeof v) {
    case "undefined":
    case "boolean":
    case "string":
    case "symbol":
      return String(v);
    case "number":
      return v !== 0 ? String(v) : Object_is(v, +0) ? "+0" : "-0";
    case "object":
      if (v === null) {
        return String(v);
      }
    case "function":
    default:
      try {
        return String(v);
      } catch (e) {
        try {
          return $CallFunction(Object_prototype_toString, v);
        } catch (e) {
          return "???";
        }
      }
  }
}

function formatString(callSite, ...substitutions) {
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
  let message = typeof format === "string" ? format : formatString(format, ...args);
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

function assertThrows(expected, f, message = "") {
  try {
    f();
  } catch (e) {
    if (e instanceof expected) {
      return;
    }
    if (e instanceof AssertionError) {
      throw e;
    }
    fail `${label(message)}Expected error «${expected.name}», but got «${e}»`;
  }
  fail `${label(message)}Expected error «${expected.name}»`;
}

function assertSyntaxError(code, message = "") {
  return assertThrows(SyntaxError, () => Function(code), message);
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

function assertDataProperty(objectOrValue, propertyKey, {value, writable, enumerable, configurable}) {
  let desc = Object_getOwnPropertyDescriptor(objectOrValue, propertyKey);
  assertNotUndefined(desc, `${PropertyKeyToString(propertyKey)} not found`);
  assertTrue(IsDataPropertyDescriptor(desc), `${PropertyKeyToString(propertyKey)}.[[Value]] present`);
  assertSame(value, desc.value, `${PropertyKeyToString(propertyKey)}.[[Value]]`);
  assertSame(writable, desc.writable, `${PropertyKeyToString(propertyKey)}.[[Writable]]`);
  assertSame(enumerable, desc.enumerable, `${PropertyKeyToString(propertyKey)}.[[Enumerable]]`);
  assertSame(configurable, desc.configurable, `${PropertyKeyToString(propertyKey)}.[[Configurable]]`);
}

// Internal: Similar to assertDataProperty, except assertEquals is used for "value"
function assertDataPropertyEquals(objectOrValue, propertyKey, {value, writable, enumerable, configurable}) {
  let desc = Object_getOwnPropertyDescriptor(objectOrValue, propertyKey);
  assertNotUndefined(desc, `${PropertyKeyToString(propertyKey)} not found`);
  assertTrue(IsDataPropertyDescriptor(desc), `${PropertyKeyToString(propertyKey)}.[[Value]] present`);
  assertEquals(value, desc.value, `${PropertyKeyToString(propertyKey)}.[[Value]]`);
  assertSame(writable, desc.writable, `${PropertyKeyToString(propertyKey)}.[[Writable]]`);
  assertSame(enumerable, desc.enumerable, `${PropertyKeyToString(propertyKey)}.[[Enumerable]]`);
  assertSame(configurable, desc.configurable, `${PropertyKeyToString(propertyKey)}.[[Configurable]]`);
}

function assertAccessorProperty(objectOrValue, propertyKey, {get, set, enumerable, configurable}) {
  let desc = Object_getOwnPropertyDescriptor(objectOrValue, propertyKey);
  assertNotUndefined(desc, `${PropertyKeyToString(propertyKey)} not found`);
  assertFalse(IsDataPropertyDescriptor(desc), `${PropertyKeyToString(propertyKey)}.[[Value]] present`);
  assertSame(get, desc.get, `${PropertyKeyToString(propertyKey)}.[[Get]]`);
  assertSame(set, desc.set, `${PropertyKeyToString(propertyKey)}.[[Set]]`);
  assertSame(enumerable, desc.enumerable, `${PropertyKeyToString(propertyKey)}.[[Enumerable]]`);
  assertSame(configurable, desc.configurable, `${PropertyKeyToString(propertyKey)}.[[Configurable]]`);
}

function assertBuiltinFunction(fun, name, arity) {
  assertSame("function", typeof fun);
  assertCallable(fun);
  assertNotConstructor(fun);
  assertSame(Function.prototype, Reflect_getPrototypeOf(fun), `${name}.[[Prototype]]`);
  assertDataProperty(fun, "length", {value: arity, writable: false, enumerable: false, configurable: true});
  if (name !== void 0) {
    assertDataProperty(fun, "name", {value: name, writable: false, enumerable: false, configurable: true});
  } else {
    // anonymous function
    assertUndefined(Reflect_getOwnPropertyDescriptor(fun, "name"));
  }
  assertUndefined(Reflect_getOwnPropertyDescriptor(fun, "arguments"));
  assertUndefined(Reflect_getOwnPropertyDescriptor(fun, "caller"));
}

function assertNativeFunction(fun, name, arity) {
  assertBuiltinFunction(fun, name, arity);
  let source = $CallFunction(Function_prototype_toString, fun);
  assertTrue($CallFunction(String_prototype_includes, source, "native code"));
}

function assertBuiltinConstructor(fun, name, arity) {
  assertSame("function", typeof fun);
  assertCallable(fun);
  assertConstructor(fun);
  assertSame(Function.prototype, Reflect_getPrototypeOf(fun), `${name}.[[Prototype]]`);
  assertDataProperty(fun, "length", {value: arity, writable: false, enumerable: false, configurable: true});
  assertDataProperty(fun, "name", {value: name, writable: false, enumerable: false, configurable: true});
  assertUndefined(Reflect_getOwnPropertyDescriptor(fun, "arguments"));
  assertUndefined(Reflect_getOwnPropertyDescriptor(fun, "caller"));
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
  assertSame(proto, Reflect_getPrototypeOf(o), `[[Prototype]]`);
}

function assertEquals(expected, actual, message = "") {
  if (IsPrimitive(expected)) {
    return assertSame(expected, actual, message);
  }
  assertNotNull(actual, message);
  assertSame(typeof expected, typeof actual, message);
  if (Object_is(expected, actual)) {
    return;
  }

  let expectedOwnKeys = Reflect_ownKeys(expected);
  let actualOwnKeys = Reflect_ownKeys(actual);
  let actualOwnKeysSet = new PropertyKeySet(actualOwnKeys);
  for (let i = 0, len = expectedOwnKeys.length; i < len; ++i) {
    let propertyKey = expectedOwnKeys[i];
    let actualHas = actualOwnKeysSet.delete(propertyKey);
    assertTrue(actualHas, `${PropertyKeyToString(propertyKey)} not present`);

    let expectedDesc = Reflect_getOwnPropertyDescriptor(expected, propertyKey);
    assertNotUndefined(expectedDesc, `${PropertyKeyToString(propertyKey)} not found`);

    if (IsDataPropertyDescriptor(expectedDesc)) {
      assertDataPropertyEquals(actual, propertyKey, expectedDesc);
    } else {
      assertAccessorProperty(actual, propertyKey, expectedDesc);
    }
  }
  assertSame(0, actualOwnKeysSet.size, actualOwnKeysSet);

  return assertEquals(Reflect_getPrototypeOf(expected), Reflect_getPrototypeOf(actual));
}

return {
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
  assertBuiltinFunction, assertNativeFunction, assertBuiltinConstructor, assertBuiltinPrototype,
  assertEquals,
};

}(this);
