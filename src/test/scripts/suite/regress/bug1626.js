/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertFalse, assertTrue, assertDataProperty
} = Assert;

// 11.1.9, GetTemplateCallSite: [[Enumerable]] attribute not defined for property descriptors
// https://bugs.ecmascript.org/show_bug.cgi?id=1626

let called = false;
function handler(callSite) {
  assertFalse(called);
  called = true;

  assertDataProperty(callSite, "0", {value: "abc", writable: false, enumerable: true, configurable: false});
  assertDataProperty(callSite, "length", {value: 1, writable: false, enumerable: false, configurable: false});

  assertDataProperty(callSite.raw, "0", {value: "abc", writable: false, enumerable: true, configurable: false});
  assertDataProperty(callSite.raw, "length", {value: 1, writable: false, enumerable: false, configurable: false});
}

handler `abc`;

assertTrue(called);
