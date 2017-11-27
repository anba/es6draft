/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 22.1.3.*: Change [[DefineOwnProperty]] to DefinePropertyOrThrow
// https://bugs.ecmascript.org/show_bug.cgi?id=2065

function Ctor() {
  return Object.defineProperty({}, 0, {value: null});
}
Ctor[Symbol.species] = Ctor;

let emptyArray = [];
let nonEmptyArray = [1];
emptyArray.constructor = Ctor;
nonEmptyArray.constructor = Ctor;

assertThrows(TypeError, () => emptyArray.concat({}));
assertThrows(TypeError, () => nonEmptyArray.concat());
assertThrows(TypeError, () => nonEmptyArray.filter(() => true));
assertThrows(TypeError, () => nonEmptyArray.map(v => v));
assertThrows(TypeError, () => nonEmptyArray.splice(0));
