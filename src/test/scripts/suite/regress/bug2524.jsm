/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue, assertDataProperty
} = Assert;

// Module toString
// https://bugs.ecmascript.org/show_bug.cgi?id=2524

import* as self from "./bug2524.jsm";

assertSame("[object Module]", Object.prototype.toString.call(self));
assertTrue(Object.prototype.hasOwnProperty.call(self, Symbol.toStringTag));
assertDataProperty(self, Symbol.toStringTag, {value: "Module", writable: false, enumerable: false, configurable: false});
