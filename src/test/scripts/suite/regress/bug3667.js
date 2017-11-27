/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, fail
} = Assert;

// 24.3.2.1 Str : Missing ReturnIfAbrupt calls
// https://bugs.ecmascript.org/show_bug.cgi?id=3667

class Err extends Error { }

var numberObject = Object.assign(new Number, {
  toString: () => fail `unreachable`,
  valueOf: () => { throw new Err },
});
assertThrows(Err, () => JSON.stringify(numberObject));

var stringObject = Object.assign(new String, {
  toString: () => { throw new Err },
  valueOf: () => fail `unreachable`,
});
assertThrows(Err, () => JSON.stringify(stringObject));
