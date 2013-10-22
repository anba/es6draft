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

/* 19.1  Object Objects */

assertBuiltinConstructor(Object, "Object", 1);
assertBuiltinPrototype(Object.prototype, null);
assertSame(Object, Object.prototype.constructor);


/* 19.1.3  Properties of the Object Constructor */

assertDataProperty(Object, "assign", {value: Object.assign, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "create", {value: Object.create, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "defineProperties", {value: Object.defineProperties, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "defineProperty", {value: Object.defineProperty, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "freeze", {value: Object.freeze, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "getOwnPropertyDescriptor", {value: Object.getOwnPropertyDescriptor, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "getOwnPropertyNames", {value: Object.getOwnPropertyNames, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "getOwnPropertySymbols", {value: Object.getOwnPropertySymbols, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "getPrototypeOf", {value: Object.getPrototypeOf, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "is", {value: Object.is, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "isExtensible", {value: Object.isExtensible, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "isFrozen", {value: Object.isFrozen, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "isSealed", {value: Object.isSealed, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "keys", {value: Object.keys, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "mixin", {value: Object.mixin, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "preventExtensions", {value: Object.preventExtensions, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "prototype", {value: Object.prototype, writable: false, enumerable: false, configurable: false});
assertDataProperty(Object, "seal", {value: Object.seal, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object, "setPrototypeOf", {value: Object.setPrototypeOf, writable: true, enumerable: false, configurable: true});


/* 19.1.4  Properties of the Object Prototype Object */

assertDataProperty(Object.prototype, "constructor", {value: Object.prototype.constructor, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object.prototype, "hasOwnProperty", {value: Object.prototype.hasOwnProperty, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object.prototype, "isPrototypeOf", {value: Object.prototype.isPrototypeOf, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object.prototype, "propertyIsEnumerable", {value: Object.prototype.propertyIsEnumerable, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object.prototype, "toLocaleString", {value: Object.prototype.toLocaleString, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object.prototype, "toString", {value: Object.prototype.toString, writable: true, enumerable: false, configurable: true});
assertDataProperty(Object.prototype, "valueOf", {value: Object.prototype.valueOf, writable: true, enumerable: false, configurable: true});

