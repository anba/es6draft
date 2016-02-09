/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 22.1.3.12 Array.prototype.join: Missing ReturnIfAbrupt after step 6
// https://bugs.ecmascript.org/show_bug.cgi?id=3711

class Err extends Error { }

assertThrows(Err, () => [].join({
  toString() { throw new Err }
}));
assertThrows(Err, () => [].join({
  toString() { throw new Err },
  valueOf: null
}));
assertThrows(Err, () => [].join({
  toString: null,
  valueOf() { throw new Err }
}));
