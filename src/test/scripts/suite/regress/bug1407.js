/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame
} = Assert;

// 15.7.1.1: `Number(undefined)` no longer returns NaN
// https://bugs.ecmascript.org/show_bug.cgi?id=1407

assertSame(NaN, Number(undefined));
