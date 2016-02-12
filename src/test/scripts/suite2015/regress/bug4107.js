/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertEquals
} = Assert;

// 9.4.3.3 [[Enumerate]], 9.4.5.6 [[Enumerate]]: Duplicate property names not detected
// https://bugs.ecmascript.org/show_bug.cgi?id=4107

Object.defineProperty(Object.prototype, "0", {enumerable: true});

assertEquals(["0", "1", "2"], [...Reflect.enumerate(new String("abc"))]);
assertEquals(["0", "1", "2"], [...Reflect.enumerate(new Int8Array(3))]);
