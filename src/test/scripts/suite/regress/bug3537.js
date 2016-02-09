/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 22.1.3.25 Array.prototype.splice, 22.1.3.28 Array.prototype.unshift: Incorrect bounds check
// https://bugs.ecmascript.org/show_bug.cgi?id=3537

var array = {length: Number.MAX_SAFE_INTEGER};
Array.prototype.splice.call(array, 0, 0);

var array = {length: Number.MAX_SAFE_INTEGER};
Array.prototype.splice.call(array, 0, 1, "a");

var array = {length: Number.MAX_SAFE_INTEGER};
assertThrows(TypeError, () => Array.prototype.splice.call(array, 0, 0, "a"));

var array = {length: Number.MAX_SAFE_INTEGER};
Array.prototype.unshift.call(array);

var array = {length: Number.MAX_SAFE_INTEGER};
assertThrows(TypeError, () => Array.prototype.unshift.call(array, "a"));
