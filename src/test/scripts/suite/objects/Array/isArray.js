/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue,
  assertFalse,
  assertBuiltinFunction,
  assertThrows,
  fail
} = Assert;

/* 22.1.2.2 Array.isArray ( arg ) */

assertBuiltinFunction(Array.isArray, "isArray", 1);

// Test Array.isArray with proxies

assertTrue(Array.isArray(new Proxy([], {})));
assertTrue(Array.isArray(new Proxy(new Proxy([], {}), {})));

var {proxy, revoke} = Proxy.revocable([], {});
assertTrue(Array.isArray(proxy));
revoke();
assertThrows(TypeError, () => Array.isArray(proxy));

var {proxy, revoke} = Proxy.revocable({}, {});
assertFalse(Array.isArray(proxy));
revoke();
assertThrows(TypeError, () => Array.isArray(proxy));

var {proxy, revoke} = Proxy.revocable(function(){}, {});
assertFalse(Array.isArray(proxy));
revoke();
assertThrows(TypeError, () => Array.isArray(proxy));

Array.isArray(new Proxy([], new Proxy({}, {
  get() {
    fail `isArray should not invoke a trap`;
  }
})));
