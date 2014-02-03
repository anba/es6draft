/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 15.4.4.10 Array.prototype.slice: last step sets wrong "length"
// https://bugs.ecmascript.org/show_bug.cgi?id=1402

assertSame(1, [0, 1, 2, 3, 4].slice(1, 2).length);
