/*
 * Copyright (c) 2012-2014 André Bargull
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

/* Realm Objects */

assertBuiltinConstructor(Reflect.Realm, "Realm", 0);
assertBuiltinPrototype(Reflect.Realm.prototype);
assertSame(Reflect.Realm, Reflect.Realm.prototype.constructor);


/* Properties of the Realm Constructor */

assertPrototypeProperty(Reflect.Realm);


/* Properties of the Realm Prototype Object */

assertConstructorProperty(Reflect.Realm.prototype);
assertGetterProperty(Reflect.Realm.prototype, "global");
assertFunctionProperty(Reflect.Realm.prototype, "eval");
assertDataProperty(Reflect.Realm.prototype, Symbol.toStringTag, {value: "Reflect.Realm", writable: false, enumerable: false, configurable: true});
