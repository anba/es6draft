/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertUndefined, assertDataProperty, assertThrows
} = Assert;

// 6.1.7.3, 9.4.3: String exotic objects can violate [[GetOwnProperty]] invariant
// https://bugs.ecmascript.org/show_bug.cgi?id=2488

// Create uninitialized string object and define its "length" property
let str = new class extends String { constructor() { /* no super */ } };
Reflect.defineProperty(str, "length", {value: 1, writable: false, enumerable: false, configurable: false});

// Make string object non-extensible, observe "0" property
Reflect.preventExtensions(str);
assertFalse(Reflect.isExtensible(str));
assertUndefined(Reflect.getOwnPropertyDescriptor(str, "0"));

// Initialize string object and retrieve "0" property
assertThrows(TypeError, () => String.call(str, "A"));
assertUndefined(Reflect.getOwnPropertyDescriptor(str, "0"));
