/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 21.1.3.16 String.prototype.slice: Missing ReturnIfAbrupt after step 5 & 6
// https://bugs.ecmascript.org/show_bug.cgi?id=3714

class Err extends Error { }

var thrower = {valueOf() { throw new Err() }};

assertThrows(Err, () => "".slice(thrower));
assertThrows(Err, () => "".slice(0, thrower));
