/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, fail
} = Assert;

// 24.3.2 JSON.stringify: Missing ReturnIfAbrupt calls
// https://bugs.ecmascript.org/show_bug.cgi?id=3666

class Err extends Error { }

var stringObject = Object.assign(new String, {
  toString: () => { throw new Err },
  valueOf: () => fail `unreachable`,
});
assertThrows(Err, () => JSON.stringify(null, [stringObject]));

var numberObject = Object.assign(new String, {
  toString: () => { throw new Err },
  valueOf: () => fail `unreachable`,
});
assertThrows(Err, () => JSON.stringify(null, [numberObject]));

var numberObject = Object.assign(new Number, {
  toString: () => fail `unreachable`,
  valueOf: () => { throw new Err },
});
assertThrows(Err, () => JSON.stringify(null, null, numberObject));

var stringObject = Object.assign(new String, {
  toString: () => { throw new Err },
  valueOf: () => fail `unreachable`,
});
assertThrows(Err, () => JSON.stringify(null, null, stringObject));
