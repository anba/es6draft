/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 20.3.4.35 Date.prototype.toDateString, 20.3.4.42 Date.prototype.toTimeString: Handle [[DateValue]] = NaN ?
// https://bugs.ecmascript.org/show_bug.cgi?id=4342

assertSame("Invalid Date", new Date(NaN).toDateString());
assertSame("Invalid Date", new Date(NaN).toTimeString());
assertSame("Invalid Date", new Date(NaN).toUTCString());
assertSame("Invalid Date", new Date(NaN).toGMTString());
