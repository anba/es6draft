/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse
} = Assert;

// 7.3.15 TestIntegrityLevel: Only test for [[Writable]] when level = "frozen"
// https://bugs.ecmascript.org/show_bug.cgi?id=3758

var obj = {};
Object.defineProperty(obj, "p", {
  value: 0, writable: true, configurable: false
});
Object.preventExtensions(obj);
assertTrue(Object.isSealed(obj));
assertFalse(Object.isFrozen(obj));

var obj = {};
Object.defineProperty(obj, "p", {
  value: 0, writable: false, configurable: false
});
Object.preventExtensions(obj);
assertTrue(Object.isSealed(obj));
assertTrue(Object.isFrozen(obj));
