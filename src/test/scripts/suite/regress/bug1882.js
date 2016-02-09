/*
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.4.3.10: Missing count = max(count, 0) step
// https://bugs.ecmascript.org/show_bug.cgi?id=1882

assertSame(0, [1, 2, 3].slice(1, 0).length);
assertSame(0, new Int8Array(10).slice(1, 0).length);
