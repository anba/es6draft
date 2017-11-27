/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// B.2.3.1 String.prototype.substr: Missing ReturnIfAbrupt after step 2
// https://bugs.ecmascript.org/show_bug.cgi?id=3710

assertThrows(TypeError, () => String.prototype.substr.call(void 0));
assertThrows(TypeError, () => String.prototype.substr.call(null));

class Err extends Error { }
assertThrows(Err, () => String.prototype.substr.call({
  toString() { throw new Err }
}));
assertThrows(Err, () => String.prototype.substr.call({
  toString() { throw new Err },
  valueOf: null
}));
assertThrows(Err, () => String.prototype.substr.call({
  toString: null,
  valueOf() { throw new Err }
}));
