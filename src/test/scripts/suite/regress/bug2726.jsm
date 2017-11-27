/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows, assertDataProperty
} = Assert;

// 9.4.6.5 [[GetOwnProperty]] (P): Implementation does not match module object description
// https://bugs.ecmascript.org/show_bug.cgi?id=2726

import* as self from "./bug2726.jsm";
export var a = 123;

let keys = Object.getOwnPropertyNames(self);
assertSame(1, keys.length);
assertSame("a", keys[0]);
assertDataProperty(self, "a", {value: 123, writable: true, enumerable: true, configurable: false});
