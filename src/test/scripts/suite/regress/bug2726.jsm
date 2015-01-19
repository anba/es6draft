/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 9.4.6.5 [[GetOwnProperty]] (P): Implementation does not match module object description
// https://bugs.ecmascript.org/show_bug.cgi?id=2726

import* as self from "./bug2726.jsm";
export var a = 123;

let keys = [...Reflect.enumerate(self)];
assertSame(1, keys.length);
assertSame("a", keys[0]);
assertThrows(TypeError, () => Object.getOwnPropertyDescriptor(self, "a"));
