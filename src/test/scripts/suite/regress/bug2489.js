/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertTrue, assertFalse,
} = Assert;

// 6.1.7.3, 9.1.2: Interleaved Proxy handler calls can violate [[SetPrototypeOf]] invariant
// https://bugs.ecmascript.org/show_bug.cgi?id=2489

let observedProto;
let obj = {};
let proxy = new Proxy({}, {
  getPrototypeOf(t) {
    // Interleaved Proxy handler call
    assertTrue(Reflect.isExtensible(obj));
    // Make object non-extensible and retrieve [[Prototype]]
    Reflect.preventExtensions(obj);
    observedProto = Reflect.getPrototypeOf(obj);
    assertFalse(Reflect.isExtensible(obj));
    return Reflect.getPrototypeOf(t);
  }
});

// Change [[Prototype]] of `obj`
assertTrue(Reflect.isExtensible(obj));
let setProtoResult = Reflect.setPrototypeOf(obj, proxy);
assertFalse(Reflect.isExtensible(obj));

// Inspect current [[Prototype]] of `obj`
let currentProto = Reflect.getPrototypeOf(obj);
assertSame(observedProto, currentProto);
assertFalse(setProtoResult);
