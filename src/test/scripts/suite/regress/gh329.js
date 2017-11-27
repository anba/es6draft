/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertFalse, assertTrue, fail
} = Assert;

// Proxy [[SetPrototypeOf]] improvement
// https://github.com/tc39/ecma262/pull/329

var setPrototypeOfCalled = false;

var t = new Proxy({}, {
  isExtensible() {
    fail `isExtensible trap called`;
  }
});

var p = new Proxy(t, {
  setPrototypeOf() {
    assertFalse(setPrototypeOfCalled);
    setPrototypeOfCalled = true;
    return false;
  }
});

assertFalse(setPrototypeOfCalled);
Reflect.setPrototypeOf(p, {});
assertTrue(setPrototypeOfCalled);
