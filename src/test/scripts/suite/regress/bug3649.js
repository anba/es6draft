/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 22.1.3.1 Array.prototype.concat: Incorrect bound checks
// https://bugs.ecmascript.org/show_bug.cgi?id=3649

class Base extends Array {
  static get [Symbol.species]() {
    return function(){};
  }
}

var array = {[Symbol.isConcatSpreadable]: true, length: Number.MAX_SAFE_INTEGER};
var result = Array.prototype.concat.call(new Base, array);
assertSame(Number.MAX_SAFE_INTEGER, result.length);

var array = {[Symbol.isConcatSpreadable]: true, length: Number.MAX_SAFE_INTEGER + 1};
var result = Array.prototype.concat.call(new Base, array);
assertSame(Number.MAX_SAFE_INTEGER, result.length);

var array = {[Symbol.isConcatSpreadable]: true, length: Number.MAX_SAFE_INTEGER + 2};
var result = Array.prototype.concat.call(new Base, array);
assertSame(Number.MAX_SAFE_INTEGER, result.length);
