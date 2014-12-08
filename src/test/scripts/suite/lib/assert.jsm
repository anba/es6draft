/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const global = this;

const {
  Object, Function, Proxy, Reflect, Set, String, Symbol, SyntaxError
} = global;

const {
  getOwnPropertyDescriptor: Reflect_getOwnPropertyDescriptor,
  getPrototypeOf: Reflect_getPrototypeOf,
  ownKeys: Reflect_ownKeys,
} = Reflect;

const {
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
  contains: String_prototype_contains,
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
    this._set = new Set();
    for (let i = 0, len = keys.length; i < len; ++i) {
      this.add(keys[i]);
    }
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

export class AssertionError extends Error {
  get name() {
    return "AssertionError";
  }
}

export function fail(format = "", ...args) {
  let message = typeof format === "string" ? format : formatString(format, ...args);
  throw new AssertionError(message);
}

export function assertSame(expected, actual, message = "") {
  if (!Object_is(expected, actual)) {
    fail `${label(message)}Expected «${expected}», but got «${actual}»`;
  }
}

export function assertNotSame(expected, actual, message = "") {
  if (Object_is(expected, actual)) {
    fail `${label(message)}Expected not «${expected}»`;
  }
}

export function assertThrows(expected, f, message = "") {
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

export function assertSyntaxError(code, message = "") {
  return assertThrows(SyntaxError, () => Function(code), message);
}

export function assertTrue(actual, message = "") {
  return assertSame(true, actual, message);
}

export function assertFalse(actual, message = "") {
  return assertSame(false, actual, message);
}

export function assertUndefined(actual, message = "") {
  return assertSame(void 0, actual, message);
}

export function assertNotUndefined(actual, message = "") {
  return assertNotSame(void 0, actual, message);
}

export function assertNull(actual, message = "") {
  return assertSame(null, actual, message);
}

export function assertNotNull(actual, message = "") {
  return assertNotSame(null, actual, message);
}

export function assertInstanceOf(constructor, value, message = `Is an instance of ${constructor.name}`) {
  return assertTrue(value instanceof constructor, message);
}

export function assertCallable(o, message = "Is a [[Callable]] object") {
  return assertTrue(IsCallable(o), message);
}

export function assertNotCallable(o, message = "Is not a [[Callable]] object") {
  return assertFalse(IsCallable(o), message);
}

export function assertConstructor(o, message = "Is a [[Constructor]] object") {
  return assertTrue(IsConstructor(o), message);
}

export function assertNotConstructor(o, message = "Is not a [[Constructor]] object") {
  return assertFalse(IsConstructor(o), message);
}

export function assertDataProperty(object, propertyKey, {value, writable, enumerable, configurable}) {
  let desc = Reflect_getOwnPropertyDescriptor(object, propertyKey);
  assertNotUndefined(desc, `${PropertyKeyToString(propertyKey)} not found`);
  assertTrue(IsDataPropertyDescriptor(desc), `${PropertyKeyToString(propertyKey)}.[[Value]] present`);
  assertSame(value, desc.value, `${PropertyKeyToString(propertyKey)}.[[Value]]`);
  assertSame(writable, desc.writable, `${PropertyKeyToString(propertyKey)}.[[Writable]]`);
  assertSame(enumerable, desc.enumerable, `${PropertyKeyToString(propertyKey)}.[[Enumerable]]`);
  assertSame(configurable, desc.configurable, `${PropertyKeyToString(propertyKey)}.[[Configurable]]`);
}

// Internal: Similar to assertDataProperty, except assertEquals is used for "value"
function assertDataPropertyEquals(object, propertyKey, {value, writable, enumerable, configurable}) {
  let desc = Reflect_getOwnPropertyDescriptor(object, propertyKey);
  assertNotUndefined(desc, `${PropertyKeyToString(propertyKey)} not found`);
  assertTrue(IsDataPropertyDescriptor(desc), `${PropertyKeyToString(propertyKey)}.[[Value]] present`);
  assertEquals(value, desc.value, `${PropertyKeyToString(propertyKey)}.[[Value]]`);
  assertSame(writable, desc.writable, `${PropertyKeyToString(propertyKey)}.[[Writable]]`);
  assertSame(enumerable, desc.enumerable, `${PropertyKeyToString(propertyKey)}.[[Enumerable]]`);
  assertSame(configurable, desc.configurable, `${PropertyKeyToString(propertyKey)}.[[Configurable]]`);
}

export function assertAccessorProperty(object, propertyKey, {get, set, enumerable, configurable}) {
  let desc = Reflect_getOwnPropertyDescriptor(object, propertyKey);
  assertNotUndefined(desc, `${PropertyKeyToString(propertyKey)} not found`);
  assertFalse(IsDataPropertyDescriptor(desc), `${PropertyKeyToString(propertyKey)}.[[Value]] present`);
  assertSame(get, desc.get, `${PropertyKeyToString(propertyKey)}.[[Get]]`);
  assertSame(set, desc.set, `${PropertyKeyToString(propertyKey)}.[[Set]]`);
  assertSame(enumerable, desc.enumerable, `${PropertyKeyToString(propertyKey)}.[[Enumerable]]`);
  assertSame(configurable, desc.configurable, `${PropertyKeyToString(propertyKey)}.[[Configurable]]`);
}

export function assertBuiltinFunction(fun, name, arity) {
  assertSame("function", typeof fun);
  assertCallable(fun);
  assertNotConstructor(fun);
  assertSame(Function.prototype, Reflect_getPrototypeOf(fun), `${name}.[[Prototype]]`);
  assertDataProperty(fun, "length", {value: arity, writable: false, enumerable: false, configurable: true});
  if (name !== void 0) {
    assertDataProperty(fun, "name", {value: name, writable: false, enumerable: false, configurable: true});
  } else {
    // anonymous function
    assertFalse($CallFunction(Object_prototype_hasOwnProperty, fun, "name"));
  }
  assertUndefined(Reflect_getOwnPropertyDescriptor(fun, "arguments"));
  assertUndefined(Reflect_getOwnPropertyDescriptor(fun, "caller"));
}

export function assertNativeFunction(fun, name, arity) {
  assertBuiltinFunction(fun, name, arity);
  let source = $CallFunction(Function_prototype_toString, fun);
  assertTrue($CallFunction(String_prototype_contains, source, "native code"));
}

export function assertBuiltinConstructor(fun, name, arity) {
  assertSame("function", typeof fun);
  assertCallable(fun);
  assertConstructor(fun);
  assertSame(Function.prototype, Reflect_getPrototypeOf(fun), `${name}.[[Prototype]]`);
  assertDataProperty(fun, "length", {value: arity, writable: false, enumerable: false, configurable: true});
  assertDataProperty(fun, "name", {value: name, writable: false, enumerable: false, configurable: true});
  assertUndefined(Reflect_getOwnPropertyDescriptor(fun, "arguments"));
  assertUndefined(Reflect_getOwnPropertyDescriptor(fun, "caller"));
}

export function assertBuiltinPrototype(o, proto = Object.prototype) {
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

export function assertEquals(expected, actual, message = "") {
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
