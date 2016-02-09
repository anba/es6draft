/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, fail
} = Assert;

// 9.3.2 CreateBuiltinFunction: Directly assign [[Prototype]] slot instead of calling [[SetPrototypeOf]]
// https://bugs.ecmascript.org/show_bug.cgi?id=3242

var callCount = 0;
Object.setPrototypeOf(Function.prototype, new Proxy(Object.create(Object.prototype), {
  __proto__: null,
  getPrototypeOf(t) {
    fail `unreachable`;
    callCount++;
    return Reflect.getPrototypeOf(t);
  }
}));

// Called once during Object.setPrototypeOf()
assertSame(0, callCount);

function f(a) { return arguments[0] }
assertSame(0, f(0));

function g(a) { return eval("arguments[0]") }
assertSame(0, g(0));

// Not called when arguments objects are created.
assertSame(0, callCount);
