/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame, assertThrows
} = Assert;

// 9.4.2.2 ArrayCreate checks length is <=2^32-1, whereas everything else checks for Number.MAX_SAFE_INTEGER
// https://bugs.ecmascript.org/show_bug.cgi?id=3802

assertSame(Math.pow(2, 32) - 1, new Array(Math.pow(2, 32) - 1).length);
assertThrows(RangeError, () => new Array(Math.pow(2, 32)));
assertThrows(RangeError, () => new Array(Math.pow(2, 32) + 1));
