/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 22.1.3.17 Array.prototype.push: Incorrect bounds check
// https://bugs.ecmascript.org/show_bug.cgi?id=3650

var array = {length: Number.MAX_SAFE_INTEGER};
Array.prototype.push.call(array);
assertSame(Number.MAX_SAFE_INTEGER, array.length);

var array = {length: Number.MAX_SAFE_INTEGER + 1};
Array.prototype.push.call(array);
assertSame(Number.MAX_SAFE_INTEGER, array.length);

var array = {length: Number.MAX_SAFE_INTEGER + 2};
Array.prototype.push.call(array);
assertSame(Number.MAX_SAFE_INTEGER, array.length);
