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

function assertCreateFunctionProperty(object, name, value = object[name]) {
  return assertDataProperty(object, name, {value, writable: false, enumerable: false, configurable: true});
}

function assertConstructorProperty(object, name = "constructor", value = object[name]) {
  return assertDataProperty(object, name, {value, writable: true, enumerable: false, configurable: true});
}

function assertPrototypeProperty(object, name = "prototype", value = object[name]) {
  return assertDataProperty(object, name, {value, writable: false, enumerable: false, configurable: false});
}

/* Set Objects */

assertBuiltinConstructor(Set, "Set", 1);
assertBuiltinPrototype(Set.prototype);
assertSame(Set, Set.prototype.constructor);


/* Properties of the Set Constructor */

assertPrototypeProperty(Set);
assertCreateFunctionProperty(Set, Symbol.create);


/* Properties of the Set Prototype Object */

assertConstructorProperty(Set.prototype);
assertFunctionProperty(Set.prototype, "add");
assertFunctionProperty(Set.prototype, "clear");
assertFunctionProperty(Set.prototype, "delete");
assertFunctionProperty(Set.prototype, "entries");
assertFunctionProperty(Set.prototype, "forEach");
assertFunctionProperty(Set.prototype, "has");
assertSame(Set.prototype.values, Set.prototype.keys);
assertGetterProperty(Set.prototype, "size");
assertFunctionProperty(Set.prototype, "values");
assertSame(Set.prototype.values, Set.prototype[Symbol.iterator]);
assertDataProperty(Set.prototype, Symbol.toStringTag, {value: "Set", writable: false, enumerable: false, configurable: true});
