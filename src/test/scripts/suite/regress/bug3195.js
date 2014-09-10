/*
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */

const {
  assertSame, assertTrue, assertFalse
} = Assert;

// 22.1.3.24.1 SortCompare: Sort non-existent values to the end
// https://bugs.ecmascript.org/show_bug.cgi?id=3195

var array = [, void 0, ""];

array.sort();

assertTrue(0 in array);
assertSame("", array[0]);

assertTrue(1 in array);
assertSame(void 0, array[1]);

assertFalse(2 in array);
assertSame(void 0, array[2]);
