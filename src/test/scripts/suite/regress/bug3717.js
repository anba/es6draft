/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 19.5.3.4 Error.prototype.toString: Missing ReturnIfAbrupt after steps 5 & 8
// https://bugs.ecmascript.org/show_bug.cgi?id=3717

class Err extends Error { }

var thrower = {
  toString() { throw new Err() }
};

assertThrows(Err, () => Error.prototype.toString.call({
  name: thrower,
}));

assertThrows(Err, () => Error.prototype.toString.call({
  name: "",
  message: thrower,
}));
