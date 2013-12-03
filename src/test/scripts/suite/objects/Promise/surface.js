/*
 * Copyright (c) 2012-2013 André Bargull
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

function assertCreateFunctionProperty(object, name, value = object[name]) {
  return assertDataProperty(object, name, {value, writable: false, enumerable: false, configurable: true});
}

function assertConstructorProperty(object, name = "constructor", value = object[name]) {
  return assertDataProperty(object, name, {value, writable: true, enumerable: false, configurable: true});
}

function assertPrototypeProperty(object, name = "prototype", value = object[name]) {
  return assertDataProperty(object, name, {value, writable: false, enumerable: false, configurable: false});
}

/* Promise Objects */

assertBuiltinConstructor(Promise, "Promise", 1);
assertBuiltinPrototype(Promise.prototype);
assertSame(Promise, Promise.prototype.constructor);


/* Properties of the Promise Constructor */

assertPrototypeProperty(Promise);
assertCreateFunctionProperty(Promise, Symbol.create);
assertFunctionProperty(Promise, "all");
assertFunctionProperty(Promise, "cast");
assertFunctionProperty(Promise, "race");
assertFunctionProperty(Promise, "reject");
assertFunctionProperty(Promise, "resolve");


/* Properties of the Promise Prototype Object */

assertConstructorProperty(Promise.prototype);
assertFunctionProperty(Promise.prototype, "catch");
assertFunctionProperty(Promise.prototype, "then");
