/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 21.1.3.18 String.prototype.startsWith: Missing ReturnIfAbrupt after step 5
// https://bugs.ecmascript.org/show_bug.cgi?id=2901

const expected = {};
try {
  "".startsWith({toString: () => { throw expected }});
} catch (e) { caught = e }
assertSame(expected, caught);
