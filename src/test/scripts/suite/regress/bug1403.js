/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 15.9.5.45: Type conversion for hint parameter
// https://bugs.ecmascript.org/show_bug.cgi?id=1403

const invalidHints = [void 0, null, true, 0, -1, +1, 1.43, NaN, {}, []];
for (let hint of invalidHints) {
  let d = new Date();
  assertThrows(TypeError, () => d[Symbol.toPrimitive](hint));
}

const invalidHintsString = ["", "Number", "String", "stringstring"];
for (let hint of invalidHintsString) {
  let d = new Date();
  assertThrows(TypeError, () => d[Symbol.toPrimitive](hint));
}

const noToStringForHint = [
 {valueOf() { return "string" }},
 {toString() { return "string" }},
];
for (let hint of noToStringForHint) {
  let d = new Date();
  assertThrows(TypeError, () => d[Symbol.toPrimitive](hint));
}

const validHints = ["default", "string", "number"];
for (let hint of validHints) {
  let d = new Date();
  d[Symbol.toPrimitive](hint); // no error
}
