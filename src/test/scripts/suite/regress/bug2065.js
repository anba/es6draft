/*
 * Copyright (c) 2012-2014 André Bargull
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

let emptyArray = [];
let nonEmptyArray = [1];
emptyArray.constructor = Ctor;
nonEmptyArray.constructor = Ctor;

assertThrows(() => emptyArray.concat({}), TypeError);
assertThrows(() => nonEmptyArray.concat(), TypeError);
assertThrows(() => nonEmptyArray.filter(() => true), TypeError);
assertThrows(() => nonEmptyArray.map(v => v), TypeError);
assertThrows(() => nonEmptyArray.splice(0), TypeError);
