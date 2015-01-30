/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertTrue, assertFalse, assertThrows
} = Assert;

// 9.4.5.2 [[HasProperty]]: Inconsistent handling of negative indices and OOB
// https://bugs.ecmascript.org/show_bug.cgi?id=3619

var ta = new Int8Array(10);

for (let index of [0, 1, 2, 8, 9, Number.MAX_SAFE_INTEGER, Number.MAX_VALUE]) {
  assertTrue(index in ta);
  assertTrue(("" + index) in ta);
}

for (let index of [-0, -1, -2, -8, -9, Number.MIN_SAFE_INTEGER, -Number.MAX_VALUE]) {
  assertTrue(index in ta);
  assertTrue(("" + index) in ta);
}

assertFalse("-0" in ta);

for (let index of [Number.NaN, Number.NEGATIVE_INFINITY, Number.POSITIVE_INFINITY]) {
  assertFalse(index in ta);
  assertFalse(("" + index) in ta);
}

for (let index of [0.5, 1.5, 2.5, 8.5, 9.5, Number.MIN_VALUE]) {
  assertFalse(index in ta);
  assertFalse(("" + index) in ta);
}

for (let index of [-0.5, -1.5, -2.5, -8.5, -9.5, -Number.MIN_VALUE]) {
  assertFalse(index in ta);
  assertFalse(("" + index) in ta);
}

detachArrayBuffer(ta.buffer);

for (let index of [0, 1, 2, 8, 9, Number.MAX_SAFE_INTEGER, Number.MAX_VALUE]) {
  assertThrows(TypeError, () => index in ta);
  assertThrows(TypeError, () => ("" + index) in ta);
}

for (let index of [-0, -1, -2, -8, -9, Number.MIN_SAFE_INTEGER, -Number.MAX_VALUE]) {
  assertThrows(TypeError, () => index in ta);
  assertThrows(TypeError, () => ("" + index) in ta);
}

assertThrows(TypeError, () => "-0" in ta);

for (let index of [Number.NaN, Number.NEGATIVE_INFINITY, Number.POSITIVE_INFINITY]) {
  assertThrows(TypeError, () => index in ta);
  assertThrows(TypeError, () => ("" + index) in ta);
}

for (let index of [0.5, 1.5, 2.5, 8.5, 9.5, Number.MIN_VALUE]) {
  assertThrows(TypeError, () => index in ta);
  assertThrows(TypeError, () => ("" + index) in ta);
}

for (let index of [-0.5, -1.5, -2.5, -8.5, -9.5, -Number.MIN_VALUE]) {
  assertThrows(TypeError, () => index in ta);
  assertThrows(TypeError, () => ("" + index) in ta);
}
