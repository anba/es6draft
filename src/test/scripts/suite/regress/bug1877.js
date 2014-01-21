/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertEquals
} = Assert;

// 15.4.3.12: Array.prototype.splice still not web-compatible
// https://bugs.ecmascript.org/show_bug.cgi?id=1877

assertEquals([], [1,2,3].splice());
assertEquals([1,2,3], [1,2,3].splice(0));
