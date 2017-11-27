/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
  assertDataProperty,
  assertBuiltinConstructor,
  assertBuiltinPrototype,
} = Assert;

function assertFunctionProperty(object, name, value = object[name]) {
  return assertDataProperty(object, name, {value, writable: true, enumerable: false, configurable: true});
}

function assertConstructorProperty(object, name = "constructor", value = object[name]) {
  return assertDataProperty(object, name, {value, writable: true, enumerable: false, configurable: true});
}

function assertPrototypeProperty(object, name = "prototype", value = object[name]) {
  return assertDataProperty(object, name, {value, writable: false, enumerable: false, configurable: false});
}

/* 19.1  Object Objects */

assertBuiltinConstructor(Object, "Object", 1);
assertBuiltinPrototype(Object.prototype, null);
assertSame(Object, Object.prototype.constructor);


/* 19.1.3  Properties of the Object Constructor */

assertPrototypeProperty(Object);
assertFunctionProperty(Object, "assign");
assertFunctionProperty(Object, "create");
assertFunctionProperty(Object, "defineProperties");
assertFunctionProperty(Object, "defineProperty");
assertFunctionProperty(Object, "freeze");
assertFunctionProperty(Object, "getOwnPropertyDescriptor");
assertFunctionProperty(Object, "getOwnPropertyNames");
assertFunctionProperty(Object, "getOwnPropertySymbols");
assertFunctionProperty(Object, "getPrototypeOf");
assertFunctionProperty(Object, "is");
assertFunctionProperty(Object, "isExtensible");
assertFunctionProperty(Object, "isFrozen");
assertFunctionProperty(Object, "isSealed");
assertFunctionProperty(Object, "keys");
assertFunctionProperty(Object, "preventExtensions");
assertFunctionProperty(Object, "seal");
assertFunctionProperty(Object, "setPrototypeOf");


/* 19.1.4  Properties of the Object Prototype Object */

assertConstructorProperty(Object.prototype);
assertFunctionProperty(Object.prototype, "hasOwnProperty");
assertFunctionProperty(Object.prototype, "isPrototypeOf");
assertFunctionProperty(Object.prototype, "propertyIsEnumerable");
assertFunctionProperty(Object.prototype, "toLocaleString");
assertFunctionProperty(Object.prototype, "toString");
assertFunctionProperty(Object.prototype, "valueOf");

