/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 21.1.3.19 String.prototype.substring: Missing ReturnIfAbrupt after steps 5 & 6
// https://bugs.ecmascript.org/show_bug.cgi?id=3713

class Err extends Error { }

var thrower = {valueOf() { throw new Err() }};

assertThrows(Err, () => "".substring(thrower));
assertThrows(Err, () => "".substring(0, thrower));
