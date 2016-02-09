/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertUndefined, assertThrows
} = Assert;

// 22.1.3.17 Array.prototype.push: Move bounds check before loop
// https://bugs.ecmascript.org/show_bug.cgi?id=3539

var array = {length: Number.MAX_SAFE_INTEGER};
Array.prototype.push.call(array);
assertSame(Number.MAX_SAFE_INTEGER, array.length);

var array = {length: Number.MAX_SAFE_INTEGER - 1};
Array.prototype.push.call(array, "a");
assertSame(Number.MAX_SAFE_INTEGER, array.length);
assertSame("a", array[Number.MAX_SAFE_INTEGER - 1]);

var array = {length: Number.MAX_SAFE_INTEGER - 2};
Array.prototype.push.call(array, "a", "b");
assertSame(Number.MAX_SAFE_INTEGER, array.length);
assertSame("a", array[Number.MAX_SAFE_INTEGER - 2]);
assertSame("b", array[Number.MAX_SAFE_INTEGER - 1]);

var array = {length: Number.MAX_SAFE_INTEGER};
assertThrows(TypeError, () => Array.prototype.push.call(array, "a"));
assertSame(Number.MAX_SAFE_INTEGER, array.length);

var array = {length: Number.MAX_SAFE_INTEGER};
assertThrows(TypeError, () => Array.prototype.push.call(array, "a", "b"));
assertSame(Number.MAX_SAFE_INTEGER, array.length);

var array = {length: Number.MAX_SAFE_INTEGER - 1};
assertThrows(TypeError, () => Array.prototype.push.call(array, "a", "b"));
assertSame(Number.MAX_SAFE_INTEGER - 1, array.length);
assertUndefined(array[Number.MAX_SAFE_INTEGER - 1]);

var array = {length: Number.MAX_SAFE_INTEGER + 1};
Array.prototype.push.call(array);
assertSame(Number.MAX_SAFE_INTEGER, array.length);

var array = {length: Number.MAX_SAFE_INTEGER + 2};
Array.prototype.push.call(array);
assertSame(Number.MAX_SAFE_INTEGER, array.length);
