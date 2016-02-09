/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows, fail
} = Assert;

// 22.1.3.1 Array.prototype.concat: Move bounds check before loop
// https://bugs.ecmascript.org/show_bug.cgi?id=3538

class MyArray extends Array {
  static get [Symbol.species]() {
    return class {};
  }
}

var array = {length: Number.MAX_SAFE_INTEGER, [Symbol.isConcatSpreadable]: true};
Array.prototype.concat.call(new MyArray, array, []);

var array = {length: Number.MAX_SAFE_INTEGER, [Symbol.isConcatSpreadable]: true};
assertThrows(TypeError, () => Array.prototype.concat.call(new MyArray, array, [,]));

var array = {length: Number.MAX_SAFE_INTEGER - 1, [Symbol.isConcatSpreadable]: true};
var result = Array.prototype.concat.call(new MyArray, array, [123]);
assertSame(123, result[Number.MAX_SAFE_INTEGER - 1]);

var array = {length: Number.MAX_SAFE_INTEGER - 1, [Symbol.isConcatSpreadable]: true};
var checkedArray = Object.defineProperty(new Array(2), "0", {
  get() { fail `unreachable` }
});
assertThrows(TypeError, () => Array.prototype.concat.call(new MyArray, array, checkedArray));
