/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows, fail
} = Assert;

// 20.3.4.32-33 Date.prototype.setUTCXXX: Missing ReturnIfAbrupt
// https://bugs.ecmascript.org/show_bug.cgi?id=4238

class MyError extends Error { }
var thrower = {valueOf() { throw new MyError }};
var unreachable = {valueOf() { fail `unreachable` }};

assertThrows(MyError, () => new Date().setUTCMinutes(thrower, unreachable, unreachable));
assertThrows(MyError, () => new Date().setUTCMinutes(0, thrower, unreachable));
assertThrows(MyError, () => new Date().setUTCMinutes(0, 0, thrower));

assertThrows(MyError, () => new Date().setUTCMonth(thrower, unreachable));
assertThrows(MyError, () => new Date().setUTCMonth(0, thrower));

assertThrows(MyError, () => new Date().setUTCSeconds(thrower, unreachable));
assertThrows(MyError, () => new Date().setUTCSeconds(0, thrower));
