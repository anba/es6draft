/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame,
  assertDataProperty,
  assertAccessorProperty,
  assertBuiltinConstructor,
  assertBuiltinPrototype,
} = Assert;

function assertGetterProperty(object, name, getter = Object.getOwnPropertyDescriptor(object, name).get) {
  return assertAccessorProperty(object, name, {get: getter, set: void 0, enumerable: false, configurable: true});
}

function assertFunctionProperty(object, name, value = object[name]) {
  return assertDataProperty(object, name, {value, writable: true, enumerable: false, configurable: true});
}

function assertConstructorProperty(object, name = "constructor", value = object[name]) {
  return assertDataProperty(object, name, {value, writable: true, enumerable: false, configurable: true});
}

function assertPrototypeProperty(object, name = "prototype", value = object[name]) {
  return assertDataProperty(object, name, {value, writable: false, enumerable: false, configurable: false});
}

/* SharedArrayBuffer Objects */

assertBuiltinConstructor(SharedArrayBuffer, "SharedArrayBuffer", 1);
assertBuiltinPrototype(SharedArrayBuffer.prototype);
assertSame(SharedArrayBuffer, SharedArrayBuffer.prototype.constructor);


/* Properties of the SharedArrayBuffer Constructor */

assertPrototypeProperty(SharedArrayBuffer);
assertFunctionProperty(SharedArrayBuffer, "isView");
assertGetterProperty(SharedArrayBuffer, Symbol.species);


/* Properties of the SharedArrayBuffer Prototype Object */

assertConstructorProperty(SharedArrayBuffer.prototype);
assertGetterProperty(SharedArrayBuffer.prototype, "byteLength");
assertFunctionProperty(SharedArrayBuffer.prototype, "slice");
assertDataProperty(SharedArrayBuffer.prototype, Symbol.toStringTag, {
  value: "SharedArrayBuffer", writable: false, enumerable: false, configurable: true
});
