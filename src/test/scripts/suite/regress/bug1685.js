/*
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
const {
  assertSame
} = Assert;

// 15.16.1, 15.17.1: Wrong if-condition for undefined iterable
// https://bugs.ecmascript.org/show_bug.cgi?id=1685

assertSame(0, new Map(void 0).size);
assertSame(0, new Map(null).size);
assertSame(0, new Set(void 0).size);
assertSame(0, new Set(null).size);

// no error
new WeakMap(void 0);
new WeakMap(null);
new WeakSet(void 0);
new WeakSet(null);
