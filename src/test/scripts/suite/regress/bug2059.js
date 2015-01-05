/*
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertThrows
} = Assert;

// 24.1.2.1 ArrayBuffer(): argument validation accepts Infinity
// https://bugs.ecmascript.org/show_bug.cgi?id=2059

assertThrows(RangeError, () => new ArrayBuffer(Infinity));
