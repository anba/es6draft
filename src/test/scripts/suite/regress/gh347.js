/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertTrue
} = Assert;

// Integer Indexed Exotic Objects inconsistent wrt Reflect.get/set/has
// https://github.com/tc39/ecma262/pull/347

var ta = new Int8Array([1]);
var obj = {__proto__: ta};

assertTrue(0 in obj);

assertSame(1, obj[0]);

obj[0] = 2;

assertSame(2, ta[0]);
